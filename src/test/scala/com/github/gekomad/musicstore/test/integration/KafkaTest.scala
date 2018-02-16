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

package com.github.gekomad.musicstore.test.integration

import java.util.UUID

import cakesolutions.kafka.{KafkaConsumer, KafkaProducer}
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import com.github.gekomad.musicstore.utility.Kafka._
import cakesolutions.kafka.testkit.KafkaServer

class KafkaTest extends FunSuite with BeforeAndAfterAll {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val kafkaServer = new KafkaServer

  override def beforeAll() = kafkaServer.startup()

  override def afterAll() = kafkaServer.close()

  case class Msg(consumerId: Int, partition: Int, offset: Long, key: String, value: String)

  /*
  1. Create a random topic with 3 partitions
  2. Write 10 messages
  3. 2 parallel consumers read the messages
  4. Delete the topic
   */
  test("multiple consumers in parallel reading from different partitions with autocommit") {


    val topic = "test_" + UUID.randomUUID

    createTopic(topic = topic, partitionSize = 3, zookeeperHosts = s"localhost:${kafkaServer.zookeeperPort}")

    def produce = {
      val conf = KafkaProducer.Conf[String, String](keySerializer = new StringSerializer, valueSerializer = new StringSerializer,
        bootstrapServers = s"localhost:${kafkaServer.kafkaPort}", acks = "1", retries = 3, lingerMs = 1)

      val kafkaProducer = KafkaProducer(conf)


      (0 until 10).map { key =>

        val message = s"message that has key: $key"
        val record = new ProducerRecord(topic, key.toString, message)
        val startTime = System.currentTimeMillis()

        kafkaProducer.sendWithCallback(record)(_ match {
          case Success(metadata) =>

            log.debug(
              s"""message($key, $message) sent to partition(${metadata.partition}) (offset(${metadata.offset}) in ${System.currentTimeMillis() - startTime} ms""")

          case Failure(f) =>
            log.error(s"error sending message($key, $message)", f)
        })

      }

      kafkaProducer.close
    }


    final case class Consumer(id: Int) {

      log.info(s"starting consumer $id")

      val list = scala.collection.mutable.ListBuffer.empty[Msg]

      val conf = KafkaConsumer.Conf(
        keyDeserializer = new StringDeserializer,
        valueDeserializer = new StringDeserializer,
        bootstrapServers = s"localhost:${kafkaServer.kafkaPort}",
        groupId = "group.id.test",
        enableAutoCommit = true,
        autoOffsetReset = OffsetResetStrategy.EARLIEST,
        autoCommitInterval = 1000
      )
      val kafkaConsumer = KafkaConsumer(conf)

      kafkaConsumer.subscribe(List(topic).asJava)

      Future {
        while (true) {
          val records = kafkaConsumer.poll(1000.seconds.toMillis)

          records.asScala.map { record =>
            log.debug(s"""Msg(consumerId=$id, partition=${record.partition},offset=${record.offset}l,key="${record.key}",value="${record.value}"), """)
            list += Msg(id, record.partition, record.offset, record.key, record.value)
          }
        }
      }
    }


    val c1 = Consumer(1)
    val c2 = Consumer(2)

    produce

    Thread.sleep(10000)

    val tot = (c1.list ++ c2.list).toList

    val readFromConsumer = List(
      Msg(consumerId = 2, partition = 2, offset = 0l, key = "0", value = "message that has key: 0"),
      Msg(consumerId = 1, partition = 0, offset = 0l, key = "1", value = "message that has key: 1"),
      Msg(consumerId = 2, partition = 2, offset = 1l, key = "2", value = "message that has key: 2"),
      Msg(consumerId = 2, partition = 2, offset = 2l, key = "3", value = "message that has key: 3"),

      Msg(consumerId = 1, partition = 1, offset = 0l, key = "4", value = "message that has key: 4"),
      Msg(consumerId = 1, partition = 0, offset = 1l, key = "5", value = "message that has key: 5"),
      Msg(consumerId = 1, partition = 1, offset = 1l, key = "6", value = "message that has key: 6"),
      Msg(consumerId = 1, partition = 0, offset = 2l, key = "7", value = "message that has key: 7"),

      Msg(consumerId = 1, partition = 0, offset = 3l, key = "8", value = "message that has key: 8"),
      Msg(consumerId = 2, partition = 2, offset = 3l, key = "9", value = "message that has key: 9")
    )

    deleteTopic(topic, zookeeperHosts = s"localhost:${kafkaServer.zookeeperPort}")

    assert(readFromConsumer.size == tot.size)

    // the ordering is not guaranteed with partitions > 1
    assert(readFromConsumer == tot.sortBy(a => a.value))

  }

  /*
  Consumer (with auto commit = false) during first loop reads the messages but when reads the message "2"
  force an exception so don't commit. (The exception is threw only at first loop - there is the flag forceError)
  During second loop the consumer reads all messages 0,1,0,1,2,3,4,5,6,7,8,9 and commits
  */

  test("auto_commit = false and manage an error") {

    val topic = "test_" + UUID.randomUUID

    createTopic(topic = topic, partitionSize = 1, zookeeperHosts = s"localhost:${kafkaServer.zookeeperPort}")

    def produce = {

      val conf = KafkaProducer.Conf[String, String](keySerializer = new StringSerializer, valueSerializer = new StringSerializer,
        bootstrapServers = s"localhost:${kafkaServer.kafkaPort}", acks = "1", retries = 3, lingerMs = 1)
      val kafkaProducer = KafkaProducer(conf)
      val startTime = System.currentTimeMillis()

      (0 until 10).map { key =>

        val message = s"message that has key: $key"
        val record = new ProducerRecord(topic, key.toString, message)
        kafkaProducer.sendWithCallback(record)(_ match {
          case Success(metadata) =>

            log.debug(
              s"""message($key, $message) sent to partition(${metadata.partition}) (offset(${metadata.offset}) in ${System.currentTimeMillis() - startTime} ms""")

          case Failure(f) =>
            log.error(s"error sending message($key, $message)", f)
        })
        log.debug(s"sent $message")
      }

      kafkaProducer.close
    }

    final case class Consumer(id: Int) {

      log.debug(s"starting consumer $id")

      val list = scala.collection.mutable.ListBuffer.empty[Msg]

      val conf = KafkaConsumer.Conf(
        keyDeserializer = new StringDeserializer,
        valueDeserializer = new StringDeserializer,
        bootstrapServers = s"localhost:${kafkaServer.kafkaPort}",
        groupId = "group.id.test",
        enableAutoCommit = false,
        autoOffsetReset = OffsetResetStrategy.EARLIEST
      )
      val kafkaConsumer = KafkaConsumer(conf)

      kafkaConsumer.subscribe(List(topic).asJava)

      Future {
        var forceError = true
        while (true) {
          val records = kafkaConsumer.poll(100)

          Try {
            records.asScala.map { record =>
              if (forceError && record.key() == "2") {
                forceError = false
                throw new Exception("error, don't commit and clear internal offset")
              } else {
                log.debug(s"""Msg(consumerId=$id, partition=${record.partition},offset=${record.offset}l,key="${record.key}",value="${record.value}"), """)
                list += Msg(id, record.partition, record.offset, record.key, record.value)
              }
            }
          } match {
            case Success(_) =>
              if (!records.isEmpty) {
                log.debug("commit")
                kafkaConsumer.commitSync()
              }
            case f =>
              log.error("", f)
              kafkaConsumer.unsubscribe
              kafkaConsumer.subscribe(List(topic).asJava)
          }
        }
      }
    }


    val c1 = Consumer(1)

    produce

    Thread.sleep(10000)

    val tot = (c1.list).toList

    val readFromConsumer = List(
      Msg(consumerId = 1, partition = 0, offset = 0l, key = "0", value = "message that has key: 0"),
      Msg(consumerId = 1, partition = 0, offset = 1l, key = "1", value = "message that has key: 1"),

      Msg(consumerId = 1, partition = 0, offset = 0l, key = "0", value = "message that has key: 0"),
      Msg(consumerId = 1, partition = 0, offset = 1l, key = "1", value = "message that has key: 1"),
      Msg(consumerId = 1, partition = 0, offset = 2l, key = "2", value = "message that has key: 2"),
      Msg(consumerId = 1, partition = 0, offset = 3l, key = "3", value = "message that has key: 3"),
      Msg(consumerId = 1, partition = 0, offset = 4l, key = "4", value = "message that has key: 4"),
      Msg(consumerId = 1, partition = 0, offset = 5l, key = "5", value = "message that has key: 5"),
      Msg(consumerId = 1, partition = 0, offset = 6l, key = "6", value = "message that has key: 6"),
      Msg(consumerId = 1, partition = 0, offset = 7l, key = "7", value = "message that has key: 7"),
      Msg(consumerId = 1, partition = 0, offset = 8l, key = "8", value = "message that has key: 8"),
      Msg(consumerId = 1, partition = 0, offset = 9l, key = "9", value = "message that has key: 9")
    )

    deleteTopic(topic, zookeeperHosts = s"localhost:${kafkaServer.zookeeperPort}")

    assert(tot.size == readFromConsumer.size)

    assert(readFromConsumer == tot)

  }

}

