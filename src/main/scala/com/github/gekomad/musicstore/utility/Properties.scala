/*
    Copyright (C) Giuseppe Cannella

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.github.gekomad.musicstore.utility

import scala.collection.JavaConverters._

import com.github.gekomad.musicstore.utility.Net._
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s.Uri
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.language.postfixOps
import scala.util.Properties.envOrNone
import scala.util.{Success, Try}
import com.github.gekomad.musicstore.utility.DefaultableInstances._
import com.github.gekomad.musicstore.utility.Defaultable._

object Properties {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  final case class Kafka(artistTopic: List[(String, Int)], dlqTopic: (String, Int), groupId: String, bootstrapServers: String, zookeeperServer: String) {
    private val p = bootstrapServers.split(":")
    private val hostk = p(0)
    private val portk = p(1).toInt

    private val z = zookeeperServer.split(":")
    private val hostz = z(0)
    private val portz = z(1).toInt

    def check: Int = if (!serverListening(hostk, portk)) {
      log.error("KAFKA NOT RESPONDING")
      1
    } else if (!serverListening(hostz, portz)) {
      log.error("ZOOKEEPER NOT RESPONDING")
      2
    } else 0

  }

  object Kafka {

    def apply(artistTopic: List[String], dlqTopic: String, groupId: String, host: String, zookeeperServer: String): Kafka = {
      Uri.fromString(if (host.contains("://")) host else s"http://$host").getOrElse(throw new Exception).port.getOrElse(throw new Exception)
      Uri.fromString(if (zookeeperServer.contains("://")) zookeeperServer else s"http://$zookeeperServer").getOrElse(throw new Exception).port.getOrElse(throw new Exception)

      val p = artistTopic.map(_.split(":"))

      require(p.foldLeft(true)((a, b) => a && b.length == 2))

      val d = dlqTopic.split(":")
      require(d.size == 2)

      val topicList = p.map(a => (a(0), a(1).toInt))

      val o = new Kafka(topicList, (d(0), d(1).toInt), groupId, host, zookeeperServer)
      o.check
      o
    }
  }

  final case class ElasticSearch(host: Uri, index1: String, artistType: String, albumType: String) {
    def check: Boolean = if (!serverListening(host.host.get.value, host.port.getOrElse(throw new Exception))) {
      log.error("ELASTIC SEARCH NOT RESPONDING")
      false
    } else true
  }

  object ElasticSearch {
    def apply(host: String, index1: String, artistType: String, albumType: String): ElasticSearch = {
      val uri = Uri.fromString(if (host.contains("://")) host else s"http://$host")
      require(uri.isRight)
      val u: Uri = uri.getOrElse(throw new Exception)
      u.port.getOrElse(throw new Exception)
      val o = new ElasticSearch(u, index1, artistType, albumType)
      o.check
      o
    }
  }

  val CONFIG_RESOURCE = "config.resource"
  val confFile: String = sys.props.get("testing").getOrElse(System.getProperty(CONFIG_RESOURCE))
  require(null != confFile, s"config.resource is blank, use -Dconfig.resource={db_env}")

  val config: Config = ConfigFactory.load(confFile)

  val httpPort: Int = envOrNone("port").fold(config.getConfig("http").getInt("port"))(_.toInt)
  val host: String = envOrNone("host") getOrElse config.getConfig("http").getString("host")

  val kafka: Option[Kafka] = Try(config.getConfig("kafka")) match {
    case Success(kafkaConf) =>

      if (kafkaConf.getBoolean("disable")) None else {
        Some(Kafka(kafkaConf.getStringList("artistTopic").asScala.toList,
          kafkaConf.getString("dlqTopic"), kafkaConf.getString("groupId"), getOrDefault(kafkaConf.getString("bootstrapServers"), "localhost:9092"),
          getOrDefault(kafkaConf.getString("zookeeperServer"), "localhost:2181")))
      }
    case _ => None
  }

  private val elasticSearchConf: Config = config.getConfig("elasticSearch")

  val elasticSearch = ElasticSearch(elasticSearchConf.getString("host"), elasticSearchConf.getString("index1"), elasticSearchConf.getString("artistType"), elasticSearchConf.getString("albumType"))

  val dc: DatabaseConfig[JdbcProfile] = {
    require(null != confFile, s"config.resource is blank, use -Dconfig.resource={db_env}")
    log.info(s"config.resource: $confFile")
    val d = DatabaseConfig.forConfig[JdbcProfile]("dc", ConfigFactory.load(confFile))
    log.info(s"max connections: ${d.db.source.maxConnections}")
    d
  }
}

