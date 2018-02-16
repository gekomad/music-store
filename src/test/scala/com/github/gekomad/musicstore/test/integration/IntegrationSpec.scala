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

import cats.implicits._
import fs2.Task
import io.circe.generic.auto._
import io.circe.{HCursor, Json}
import io.circe.java8.time._
import io.circe.parser.decode
import io.circe.syntax._
import com.github.gekomad.musicstore.utility.Net._
import com.github.gekomad.musicstore.{BlazeHttpServer, Route, StartupServices}
import com.github.gekomad.musicstore.test.integration.Common.{log, _}
import com.github.gekomad.musicstore.utility.MyRandom._
import com.github.gekomad.musicstore.utility.Properties
import java.util.concurrent.Executors

import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.model.json.out.Artist
import com.github.gekomad.musicstore.service.ElasticService
import io.circe.Decoder.Result
import org.http4s.{dsl, _}
import org.http4s.circe._
import org.http4s.client.blaze._
import org.http4s.client.Client
import org.http4s.dsl.{Ok, _}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}
import io.circe.parser.parse

class IntegrationSpec extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {

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

  def sendJsonBadRequest(uri: Uri, body: Json)(httpClient: Client) =
    httpPut(uri, body)(httpClient) {
      case BadRequest(_) =>
        Task.now(true)
      case _ => Task.now(false)
    }.unsafeRun

  test("Insert artist and album") {

    val artistId = getRandomUUID.toString

    val httpClient: Client = PooledHttp1Client()

    //insert artist
    {

      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId

      val artistObject = ArtistPayload.random

      val workaround: Json = artistObject.asJson

      val o1 = httpPut(insertUrl, workaround)(httpClient) {
        case Created(_) =>
          Task.now(true)
        case e =>
          log.error(s"sendJsonNoContent $insertUrl $workaround", e)
          Task.now(false)
      }
      assert(o1.unsafeRun(), "insert artist")

    }

    log.debug("insert album")
    val albumId = getRandomUUID.toString

    {

      val insertUrl: Uri = TEST_SERVER_URL.withPath(ALBUM_PATH) / albumId / artistId

      val albumObject = AlbumPayload.random

      val workaround: Json = albumObject.asJson

      val o1 = httpPut(insertUrl, workaround)(httpClient) {
        case Created(_) =>
          Task.now(true)
        case e =>
          log.error(s"sendJsonNoContent $insertUrl $workaround", e)
          Task.now(false)
      }
      assert(o1.unsafeRun(), "insert album")

    }

    log.debug("read artist")
    val l: String = httpClient.expect[String](TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId).unsafeRun

    val artistList = decode[Artist](l).right.getOrElse(throw new Exception(s"err decode $l"))

    assert(artistList.id == artistId.toString)

    log.debug("read album")

    val url = TEST_SERVER_URL.withPath(ALBUM_PATH) / albumId
    val l2 = httpClient.expect[String](url).unsafeRun
    assert(l2.contains(albumId),"err")

    log.debug("read artist from elastic")
    val o = ElasticService.read(Properties.elasticSearch.index1, Properties.elasticSearch.artistType, artistId.toString).unsafeRun

    assert(o.status == Ok, "read")
    httpClient.shutdown
  }

  test("Get random product") {
    val httpClient: Client = PooledHttp1Client()
    val doc = httpClient.expect[String](TEST_SERVER_URL withPath RANDOM_ARTIST_PATH).unsafeRun
    val o = parse(doc).getOrElse(throw new Exception)

    val p1 = o.as[List[ArtistPayload]]
    assert(p1.isRight)
    val p = p1.getOrElse(throw new Exception)
    assert(p.size == 1)
    assert(p(0).name.size != 0)
    httpClient.shutdownNow()
  }

  test("Insert duration = 0 returns BadRequest") {
    val artistId = getRandomUUID.toString

    val httpClient: Client = PooledHttp1Client()

    //insert artist
    {
      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId

      val artistObject = ArtistPayload.random

      val workaround: Json = artistObject.asJson

      val o1 = httpPut(insertUrl, workaround)(httpClient) {
        case Created(_) =>
          Task.now(true)
        case e =>
          log.error(s"sendJsonNoContent $insertUrl $workaround", e)
          Task.now(false)
      }
      assert(o1.unsafeRun())
    }

    // insert album
    val albumId = getRandomUUID.toString

    {

      val insertUrl: Uri = TEST_SERVER_URL.withPath(ALBUM_PATH) / artistId / albumId

      val albumObject = AlbumPayload.random.copy(duration = 0)

      val workaround: Json = albumObject.asJson

      val o1 = httpPut(insertUrl, workaround)(httpClient) {
        case BadRequest(_) =>
          Task.now(true)
        case e =>
          log.error(s"sendJsonNoContent $insertUrl $workaround", e)
          Task.now(false)
      }
      assert(o1.unsafeRun(), "read album must returns 404")
    }
    httpClient.shutdown
  }

  test("Insert wrong name returns BadRequest") {
    val artistId = getRandomUUID.toString

    val httpClient: Client = PooledHttp1Client()
    //insert artist

    {
      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId

      val artistObject = ArtistPayload.random.copy(name = "")

      val workaround: Json = artistObject.asJson

      val o1 = httpPut(insertUrl, workaround)(httpClient) {
        case BadRequest(_) =>
          Task.now(true)
        case e =>
          log.error(s"sendJsonNoContent $insertUrl $workaround", e)
          Task.now(false)
      }
      assert(o1.unsafeRun(), "insert wrong name returns BadRequest")
    }
    httpClient.shutdown
  }

}
