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
import com.github.gekomad.musicstore.utility.Properties
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StartupServices {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  Properties.kafka.map { kafka =>

    log.info("Kafka is enabled")

    kafka.artistTopic.map(topic => 0 until topic._2).foreach { a =>
      log.info(s"instantiate Consumer #$a")

      Future(Consumers.KafkaConsumer1(kafka).consume).recover {
        case e => log.error(s"error starting consume publish topic", e)
      }
    }

    (0 until kafka.dlqTopic._2).foreach { a =>
      log.info(s"instantiate Consumer dlq #$a")

      Future(Consumers.KafkaConsumerDlq(kafka).consume).recover {
        case e => log.error(s"error starting consume dlq topic", e)
      }
    }

  }.getOrElse(log.info("Kafka is disabled"))

}
