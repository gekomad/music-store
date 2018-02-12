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
import io.circe.java8.time._
import io.circe.parser.decode
import io.circe.syntax._
import com.github.gekomad.musicstore.{BlazeHttpServer, Route, StartupServices}
import com.github.gekomad.musicstore.test.integration.Common._
import com.github.gekomad.musicstore.utility.MyRandom._
import com.github.gekomad.musicstore.utility.{Log, Properties}
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.scalameter.{Key, Warmer, config}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}
import java.util.concurrent._

import ch.qos.logback.classic.Level
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.utility.ForkJoinCommon
import com.github.gekomad.musicstore.utility.Net.httpPut
import fs2.Task

class ScalaMeter extends FunSuite with BeforeAndAfterAll {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  val blazerServer: Server = BlazeBuilder
    .bindHttp(Properties.httpPort, Properties.host)
    .mountService(Route.service, "/")
    .run

  override def beforeAll(): Unit = {
    val o = createSchema.unsafeRun().status
    assert(o == Ok, "error in create schema")
    StartupServices
  }

  override def afterAll(): Unit = {
    blazerServer.shutdownNow()
  }

  test("shutdown") {}

  ignore("scalaMeter") {

    def go(x: Int): Unit = {
      val publishEventObject = ArtistPayload.random
      val json = publishEventObject.asJson

      val uuid = getRandomUUID.toString
      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / uuid
      val httpClient: Client = PooledHttp1Client()

      val o = httpPut(insertUrl, json)(httpClient) {
        case Created(_) =>
          Task.now(true)
        case e =>
          log.error(s"sendJsonNoContent $insertUrl $json $e")
          Task.now(false)
      }
      assert(o.unsafeRun())

      val l = httpClient.expect[String](TEST_SERVER_URL.withPath(ARTIST_PATH) / uuid).unsafeRun

      assert(l.contains(uuid.toString))

      httpClient.shutdownNow()
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
