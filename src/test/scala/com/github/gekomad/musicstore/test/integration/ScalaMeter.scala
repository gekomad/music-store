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

import io.circe.generic.auto._
import io.circe.syntax._
import com.github.gekomad.musicstore.{Route, Services}
import com.github.gekomad.musicstore.test.integration.Common._
import com.github.gekomad.musicstore.utility.MyRandom._
import com.github.gekomad.musicstore.utility.{ForkJoinCommon, Log, Net, Properties}
import org.http4s._
import org.http4s.client.blaze._
import org.scalameter.{Key, Warmer, config}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}
import java.util.concurrent._
import cats.effect.IO
import ch.qos.logback.classic.Level
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.utility.Net.httpPut
import Net.defaultHeader

class ScalaMeter extends FunSuite with BeforeAndAfterAll {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val httpClient = Http1Client[IO]().unsafeRunSync

  val server = Services.blazeServer.start.unsafeRunSync()

  override def beforeAll(): Unit = {

    val s = Route.createSqlSchema.flatMap { xx =>
      xx.status match {
        case Status.Ok => IO("already exists")
        case _ => Net.body(xx)
      }
    } unsafeRunSync()

    assert(s.contains("already exists"))
  }

  override def afterAll(): Unit = {
    server.shutdown.unsafeRunSync()
  }

  test("shutdown") {}

  ignore("scalaMeter") {

    def go(x: Int): Unit = {
      log.info(s"thread #$x")
      val publishEventObject = ArtistPayload.random
      val json = publishEventObject.asJson

      val uuid = getRandomUUID.toString
      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / uuid

      val wr = httpClient.fetch(httpPut(insertUrl, json)) { x =>
        x.status match {
          case Status.Created =>

            val get = httpClient.expect[String](TEST_SERVER_URL.withPath(ARTIST_PATH) / uuid)

            IO(get.map(_.contains(uuid.toString)))

          case e =>
            log.error(s"sendJsonNoContent $insertUrl $json", e)
            fail()
        }
      }.unsafeRunSync().unsafeRunSync()
      assert(wr)

    }

    val levels = Log.bkAndsetLogLevels(Level.ERROR, List(this.getClass.getCanonicalName))

    val standardConfig = config(
      Key.exec.minWarmupRuns -> 20,
      Key.exec.maxWarmupRuns -> 100,
      Key.exec.benchRuns -> 100,
      Key.verbose -> true
    ) withWarmer new Warmer.Default

    val nThreads = 50
    val time = standardConfig measure {
      val threads: Seq[ForkJoinTask[Unit]] = for (i <- 0 until nThreads) yield ForkJoinCommon.task(go(i))
      threads.foreach(x => x.join())
    }
    Log.setLogLevels(levels)
    log.info(s"** scalameter time: $time **")
  }

}
