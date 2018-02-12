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

package com.github.gekomad.musicstore.service.kafka

import java.io

import cakesolutions.kafka.KafkaConsumer
import com.github.gekomad.musicstore.service.ProductService
import com.github.gekomad.musicstore.service.kafka.model.Avro.AvroProduct
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.Properties
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords}
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters.mapAsJavaMap
import scala.concurrent.Future
import scala.concurrent.duration._

object Consumers {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  trait Consumer {
    def consume(): Unit
  }

  trait KafkaConsumerConf extends Consumer {
    type A
    type B
    val conf: KafkaConsumer.Conf[A, B]
    val topic: String

  }

  object KafkaConsumer1 {

    import scala.collection.JavaConverters._

    trait KafkaConsumerConf1 extends KafkaConsumerConf {
      type A = String
      type B = Array[Byte]

      val conf = KafkaConsumer.Conf(ConfigFactory.parseMap(mapAsJavaMap(Map[String, AnyRef](
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> Properties.kafka.bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG -> Properties.kafka.groupId))),
        keyDeserializer = new StringDeserializer,
        valueDeserializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
      )

      val topic = com.github.gekomad.musicstore.utility.Properties.kafka.artistTopic._1

    }


    def apply() = new KafkaConsumerConf1 {

      val kafkaConsumer = KafkaConsumer(conf)


      def consume: Unit = {

        import scala.collection.JavaConverters._

        def go(records: ConsumerRecords[String, Array[Byte]]): Future[List[io.Serializable]] = {

          val read = records.asScala.map { iterator =>
            log.debug(s"Getting message from topics ${List(topic)} .............")
            val message = iterator.value
            deserializeAvro[AvroProduct](message)
          }

          val v = read.flatten
          if (v.isEmpty) Future.successful(List()) else {
            // Thread.sleep(100)
            ProductService.storeList(v.toList)
          }

        }

        kafkaConsumer.subscribe(List(topic).asJava)

        while (true) {
          log.debug(s"pooling topic: ${List(topic)}")
          val records = kafkaConsumer.poll(10.seconds.toMillis)

          go(records)
        }

        kafkaConsumer.close()
      }
    }
  }

  /////////////////////////////////////////


  object KafkaConsumerDlq {

    trait KafkaConsumerConfDlq extends KafkaConsumerConf {
      type A = String
      type B = Array[Byte]

      val conf = KafkaConsumer.Conf(ConfigFactory.parseMap(mapAsJavaMap(Map[String, AnyRef](
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> Properties.kafka.bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG -> Properties.kafka.groupId))),
        keyDeserializer = new StringDeserializer,
        valueDeserializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
      )
      val topic = com.github.gekomad.musicstore.utility.Properties.kafka.dlqTopic._1
    }

    def apply() = new KafkaConsumerConfDlq {
      val kafkaConsumer = KafkaConsumer(conf)

      def consume: Unit = {

        import scala.collection.JavaConverters._

        kafkaConsumer.subscribe(List(topic).asJava)

        while (true) {
          log.debug(s"pooling topic: ${List(topic)}")
          val records = kafkaConsumer.poll(10.seconds.toMillis)

          val read = records.asScala.map { iterator =>
            log.debug(s"Getting message from topics ${List(topic)} .............")
            val message = iterator.value
            deserializeAvro[AvroProduct](message)
          }

          val v = read.flatten
          v.foreach(a => log.debug(s"read from dlq: ${a.theType} ${a.payload}"))
        }

        kafkaConsumer.close()
      }
    }
  }


}
