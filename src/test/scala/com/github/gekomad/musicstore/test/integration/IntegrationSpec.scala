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
import io.circe.Json
import io.circe.java8.time._
import io.circe.parser.decode
import io.circe.syntax._
import com.github.gekomad.musicstore.utility.Net.{body, _}
import com.github.gekomad.musicstore.{Route, Services}
import com.github.gekomad.musicstore.test.integration.Common._
import com.github.gekomad.musicstore.utility.MyRandom._
import com.github.gekomad.musicstore.utility.Properties
import cats.effect.IO
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.model.json.out.Artist
import com.github.gekomad.musicstore.service.ElasticService
import org.http4s.{dsl, _}
import org.http4s.client.blaze._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

class IntegrationSpec extends FunSuite with BeforeAndAfter with BeforeAndAfterAll {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val httpClient = Http1Client[IO]().unsafeRunSync

  val server = Services.blazeServer.start.unsafeRunSync()

  override def beforeAll(): Unit = {

    val o = Route.createSqlSchema

    o.map { a =>
      a.status match {
        case Status.Ok =>
        case e => fail()
      }
    }
  }

  override def afterAll(): Unit = {
    server.shutdown.unsafeRunSync()
  }

  def sendJsonBadRequest(uri: Uri, body: Json) = {
    val s = httpPut(uri, body)
    httpClient.fetch(s) { x =>
      x.status match {
        case Status.BadRequest =>
          IO(true)
        case _ =>
          IO(false)
      }
    }
  }

  test("Insert artist and album") {

    val artistId = getRandomUUID.toString
    //insert artist

    {

      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId
      val artistObject = ArtistPayload.random
      val workaround: Json = artistObject.asJson
      val s = httpPut(insertUrl, workaround)
      val d = httpClient.fetch(s) { x =>
        x.status match {
          case Status.Created =>
            IO(true)
          case e =>
            log.error(s"sendJsonNoContent $insertUrl $workaround", e)
            IO(false)
        }
      }
      d.map(assert(_, "insert artist"))
    }

    log.debug("insert album")
    val albumId = getRandomUUID.toString

    {

      val insertUrl: Uri = TEST_SERVER_URL.withPath(ALBUM_PATH) / albumId / artistId
      val albumObject = AlbumPayload.random
      val workaround: Json = albumObject.asJson
      val s = httpPut(insertUrl, workaround)
      val d = httpClient.fetch(s) { x =>
        x.status match {
          case Status.Created =>
            IO(true)
          case e =>
            log.error(s"sendJsonNoContent $insertUrl $workaround", e)
            IO(false)
        }
      }
      d.map(assert(_, "insert album"))
    }

    log.debug("read artist")
    val l = httpClient.expect[String](TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId)

    l.map { d =>
      val artistList = decode[Artist](d).right.getOrElse(throw new Exception(s"err decode $l"))

      assert(artistList.id == artistId.toString)

      log.debug("read album")

      val url = TEST_SERVER_URL.withPath(ALBUM_PATH) / albumId
      val l2 = httpClient.expect[String](url)
      l2.map { ll2 =>
        assert(ll2.contains(albumId), "err")

        log.debug("read artist from elastic")
        val o = ElasticService.read(Properties.elasticSearch.index1, Properties.elasticSearch.artistType, artistId.toString)

        httpClient.fetch(o) { x =>
          x.status match {
            case Status.Ok => IO(1)
            case e => throw new Exception(e.toString)
          }
        }
      }
    }

  }

  test("Get random product") {
    import io.circe.parser.parse

    val doc: IO[String] = httpClient.expect[String](TEST_SERVER_URL withPath RANDOM_ARTIST_PATH)
    doc.map { d =>
      val o = parse(d).getOrElse(throw new Exception)

      val p1 = o.as[List[ArtistPayload]]
      assert(p1.isRight)
      val p = p1.getOrElse(throw new Exception)
      assert(p.size == 1)
      assert(p.head.name.nonEmpty)
    }
  }

  test("Insert length = 0 returns BadRequest") {
    val artistId = getRandomUUID.toString

    //insert artist
    {
      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId
      val artistObject = ArtistPayload.random
      val workaround: Json = artistObject.asJson
      val s = httpPut(insertUrl, workaround)
      val d = httpClient.fetch(s) { x =>
        x.status match {
          case Status.Created =>
            IO(true)
          case e =>
            log.error(s"sendJsonNoContent $insertUrl $workaround", e)
            IO(false)
        }
      }

      d.map(assert(_))

    }

    // insert album
    val albumId = getRandomUUID.toString

    {

      val insertUrl: Uri = TEST_SERVER_URL.withPath(ALBUM_PATH) / artistId / albumId
      val albumObject = AlbumPayload.random.copy(length = 0)
      val workaround: Json = albumObject.asJson
      val s = httpPut(insertUrl, workaround)
      val d = httpClient.fetch(s) { x =>
        x.status match {
          case Status.BadRequest =>
            IO(true)
          case e =>
            log.error(s"sendJsonNoContent $insertUrl $workaround", e)
            IO(false)
        }
      }

      d.map(assert(_, "read album must returns 404"))
    }

  }

  test("Insert wrong name returns BadRequest") {
    val artistId = getRandomUUID.toString

    //insert artist

    {
      val insertUrl: Uri = TEST_SERVER_URL.withPath(ARTIST_PATH) / artistId
      val artistObject = ArtistPayload.random.copy(name = "")
      val workaround: Json = artistObject.asJson
      val s = httpPut(insertUrl, workaround)
      val d = httpClient.fetch(s) { x =>
        x.status match {
          case Status.BadRequest =>
            IO(true)
          case e =>
            log.error(s"sendJsonNoContent $insertUrl $workaround", e)
            IO(false)
        }
      }
      d.map(assert(_, "insert wrong name returns BadRequest"))
    }

  }

}
