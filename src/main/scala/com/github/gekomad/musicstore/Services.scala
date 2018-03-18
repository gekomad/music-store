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

package com.github.gekomad.musicstore

import com.github.gekomad.musicstore.service.kafka.Consumers
import org.http4s.server.blaze.BlazeBuilder
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import com.github.gekomad.musicstore.utility.Properties

object Services {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def blazeServer = BlazeBuilder[IO].bindHttp(Properties.httpPort, Properties.host).mountService(Route.service, "/")

  def startServices = {
    Properties.kafka.fold(log.info("Kafka is disabled")) { kafkaConf =>

      log.info("Kafka is enabled")

      kafkaConf.artistTopic.flatMap(topic => 0 until topic._2).foreach { a =>
        log.info(s"instantiating Consumer #$a")
        Future(Consumers.KafkaConsumer1(kafkaConf).consume()).recover {
          case e => log.error(s"error starting consume publish topic", e)
        }
      }

      (0 until kafkaConf.dlqTopic._2).foreach { a =>
        log.info(s"instantiating Consumer dlq #$a")
        Future(Consumers.KafkaConsumerDlq(kafkaConf).consume()).recover {
          case e => log.error(s"error starting consume dlq topic", e)
        }
      }
    }
  }
}
