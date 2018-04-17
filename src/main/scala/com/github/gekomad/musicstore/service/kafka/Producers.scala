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

import cakesolutions.kafka.KafkaProducer
import com.github.gekomad.musicstore.service.kafka.model.Avro
import com.github.gekomad.musicstore.service.kafka.model.Avro.{AvroAlbum, AvroPayload, AvroProduct}
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.MyRandom._
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}
import io.circe.syntax._
import com.github.gekomad.musicstore.utility.Properties.Kafka
import io.circe.generic.auto._
import scala.collection.JavaConverters.mapAsJavaMap
import scala.concurrent.Future
import cats.instances.future._
import cats.instances.vector._
import cats.syntax.traverse._
import scala.concurrent.ExecutionContext.Implicits.global

object Producers {

  val log: Logger = LoggerFactory.getLogger(this.getClass)


  trait KafkaProducerConf {
    val conf: KafkaProducer.Conf[String, Array[Byte]]
    val topic: List[String]

    val kafkaProducer: KafkaProducer[String, Array[Byte]]

    def producer(kafkaProducer: KafkaProducer[String, Array[Byte]], article: AvroProduct, topics: List[String]): Future[Vector[RecordMetadata]]

    def upsertArtist(idArtist: String, payload: String) = {
      val jsonToString = AvroPayload(idArtist, payload).asJson.spaces2
      producer(kafkaProducer, AvroProduct(Avro.upsertArtist, jsonToString), topic)
    }

    def upsertAlbum(idArtist: String, idAlbum: String, payload: String) = {
      val jsonToString = AvroAlbum(idAlbum, AvroPayload(idArtist, payload)).asJson.spaces2
      producer(kafkaProducer, AvroProduct(Avro.upsertAlbum, jsonToString), topic)
    }

  }

  object KafkaProducer1 {

    abstract class KafkaProducerConf1 extends KafkaProducerConf

    def apply(kafka: Kafka) = new KafkaProducerConf1 {

      val conf: KafkaProducer.Conf[String, Array[Byte]] = KafkaProducer.Conf(

        ConfigFactory.parseMap(mapAsJavaMap(Map[String, AnyRef](
          ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> kafka.bootstrapServers))),
        keySerializer = new StringSerializer,
        valueSerializer = new org.apache.kafka.common.serialization.ByteArraySerializer
      )

      val kafkaProducer = KafkaProducer(conf)

      val (topic, _) = kafka.artistTopic.unzip


      def producer(kafkaProducer: KafkaProducer[String, Array[Byte]], article: AvroProduct, topics: List[String]): Future[Vector[RecordMetadata]] = {

        val serializedArticle = serializeAvro(article)
        //send
        topics.toVector.traverse { topic =>
          val record = new ProducerRecord(topic, getRandomUUID.toString, serializedArticle)
          log.debug("kafkaProducer.send record " + record)
          kafkaProducer.send(record)
        }
      }
    }
  }

  //////////////// dlq
  
  object KafkaProducerDlq {

    abstract class KafkaProducerConfDlq extends KafkaProducerConf

    def apply(kafka: Kafka) = new KafkaProducerConfDlq {

      val conf: KafkaProducer.Conf[String, Array[Byte]] = KafkaProducer.Conf(
        ConfigFactory.parseMap(mapAsJavaMap(Map[String, AnyRef](
          ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> kafka.bootstrapServers))),
        keySerializer = new StringSerializer,
        valueSerializer = new org.apache.kafka.common.serialization.ByteArraySerializer
      )

      val kafkaProducer = KafkaProducer(conf)

      val topic = List(kafka.dlqTopic._1)

      def producer(kafkaProducer: KafkaProducer[String, Array[Byte]], article: AvroProduct, topics: List[String]) = {
        val serializedArticle = serializeAvro(article)
        //send
        topics.toVector.traverse { topic =>
          val record = new ProducerRecord(topic, getRandomUUID.toString, serializedArticle)
          log.debug("kafkaProducer.send record " + record)
          kafkaProducer.send(record)
        }

      }

    }
  }


}
