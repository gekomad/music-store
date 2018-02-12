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

import com.github.gekomad.musicstore.utility.Net._
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s.Uri
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.language.postfixOps
import scala.util.Properties.envOrNone

object Properties {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  final case class Kafka(disabled: Boolean, artistTopic: (String, Int), dlqTopic: (String, Int), groupId: String, bootstrapServers: String) {
    private val p = bootstrapServers.split(":")
    private val host = p(0)
    private val port = p(1).toInt

    def check = if (!disabled && !serverListening(host, port)) {
      log.error("KAFKA NOT RESPONDING")
      false
    } else true
  }

  object Kafka {
    def apply(disabled: Boolean, artistTopic: String, dlqTopic: String, groupId: String, host: String) = {
      val uri = Uri.fromString(if (host.contains("://")) host else s"http://$host")
      require(uri.isRight)
      val u: Uri = uri.getOrElse(throw new Exception)
      u.port.getOrElse(throw new Exception)
      val p = artistTopic.split(":")
      val d = dlqTopic.split(":")
      require(p.size == 2)
      require(d.size == 2)
      val o = new Kafka(disabled, (p(0), p(1).toInt), (d(0), d(1).toInt), groupId, host)
      o.check
      o
    }
  }

  final case class ElasticSearch(host: Uri, index1: String, artistType: String, albumType: String) {
    def check = if (!serverListening(host.host.get.value, host.port.getOrElse(throw new Exception))) {
      log.error("ELASTIC SEARCH NOT RESPONDING")
      false
    } else true
  }

  object ElasticSearch {
    def apply(host: String, index1: String, artistType: String, albumType: String) = {
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

  val config = ConfigFactory.load(confFile)

  val httpPort: Int = envOrNone("port") map (_.toInt) getOrElse config.getConfig("http").getInt("port")
  val host: String = envOrNone("host") getOrElse config.getConfig("http").getString("host")

  private val kafkaConf: Config = config.getConfig("kafka")
  private val elasticSearchConf: Config = config.getConfig("elasticSearch")

  val kafka: Kafka = Kafka(kafkaConf.getBoolean("disable"), kafkaConf.getString("artistTopic"), kafkaConf.getString("dlqTopic"), kafkaConf.getString("groupId"), kafkaConf.getString("bootstrapServers"))

  val elasticSearch = ElasticSearch(elasticSearchConf.getString("host"), elasticSearchConf.getString("index1"), elasticSearchConf.getString("artistType"), elasticSearchConf.getString("albumType"))

  val dc: DatabaseConfig[JdbcProfile] = {
    require(null != confFile, s"config.resource is blank, use -Dconfig.resource={db_env}")
    log.info(s"config.resource: $confFile")
    val d = DatabaseConfig.forConfig[JdbcProfile]("dc", ConfigFactory.load(confFile))
    log.info(s"max connections: ${d.db.source.maxConnections}")
    d
  }
}

