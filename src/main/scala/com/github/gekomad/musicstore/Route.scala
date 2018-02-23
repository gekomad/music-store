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

import java.util.UUID

import cats.data.Validated.Invalid
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import io.circe.java8.time._
import io.circe._
import io.circe.generic.auto._
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.model.json.out.{Album, Artist}
import com.github.gekomad.musicstore.model.sql.Tables
import com.github.gekomad.musicstore.service._
import org.http4s.circe._
import org.http4s.dsl.{Ok, _}
import org.slf4j.{Logger, LoggerFactory}
import org.http4s._
import org.http4s.dsl._
import io.circe.syntax._
import slick.jdbc.meta.MTable

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import fs2.{Strategy, Task}
import com.github.gekomad.musicstore.utility.{MyRandom, Properties}
import com.github.gekomad.musicstore.utility.MyPredef._
import io.circe.parser.parse

import scala.concurrent.Future
import com.github.gekomad.musicstore.utility.UUIDable._
import com.github.gekomad.musicstore.utility.UUIDableInstances._

object Route {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def removeNull(x: Json): String = x.pretty(Printer.spaces2.copy(dropNullKeys = true))

  def jsonOK(s1: Json): Task[Response] = Ok(removeNull(s1)).putHeaders(Header("Content-Type", "application/json"))

  implicit val strategy: Strategy = fs2.Strategy.fromFixedDaemonPool(10)

  private def upsertAlbum(req: Request, idArtist: String, idAlbum: String) = {
    val p = req.as[String].map { json =>
      Validator.validateAlbum(json) match {
        case Invalid(ii) =>
          val ll = ii.map { x =>
            val p = Err(x.desc, x.field, x.code)
            log.error(s"invalid $p")
            p
          }
          BadRequest(ll.asJson)
        case _ =>
          val s1 = ProductService.upsertAlbum(idArtist, idAlbum, json)
          val s = s1.map(_ => Created()).recover {
            case f =>
              log.error("Error", f)
              BadRequest("Error " + f)
          }
          Task.fromFuture(s).flatMap(a => a)
      }
    }
    p.flatMap(a => a)
  }

  private def upsertArtist(req: Request, id: String) = {
    val p = req.as[String].map { json =>
      Validator.validateArtist(json) match {
        case Invalid(ii) =>
          val ll = ii.map { x =>
            val p = Err(x.desc, x.field, x.code)
            log.error(s"invalid $p")
            p
          }
          BadRequest(ll.asJson)
        case _ =>
          val s1 = ProductService.upsertArtist(id, json)
          val s = s1.map(_ => Created()).recover {
            case f =>
              log.error("Error", f)
              BadRequest("Error " + f)
          }
          Task.fromFuture(s).flatMap(a => a)
      }
    }
    p.flatMap(a => a)
  }

  val service = HttpService {

    case GET -> Root / "rest" / "create_sql_schema" =>
      log.debug(s"received create_sql_schema")

      val create = Tables.createSchema

      val o = create.flatMap { _ =>
        val read = Tables.loadSchema
        read.map { _ =>
          Ok("Create and Read schema OK")
        }.recover { case err =>
          log.error("Read schema error ", err)
          InternalServerError("Error " + err.getMessage)
        }
      }.recover {
        case err =>
          log.error("Create schema ERROR", err)
          InternalServerError("Error " + err.getMessage)
      }
      Task.fromFuture(o).flatMap(a => a)

    case GET -> Root / "rest" / "artist" / id =>
      log.debug(s"received $id")
      id.isUUID.fold(BadRequest("id is not valid")) { id =>
        val x: Future[Option[Tables.ArtistsType]] = ProductService.loadArtist(id)
        val o = x.map { record =>
          record.fold(NotFound(id))(x => jsonOK(Artist(x.id, x.name, x.url, x.activity).asJson))
        }.recover {
          case f =>
            log.error("err", f)
            InternalServerError("Error " + f)
        }
        Task.fromFuture(o).flatMap(a => a)
      }

    case GET -> Root / "rest" / "album" / id =>
      log.debug(s"received $id")
      id.isUUID.fold(BadRequest("id is not valid")) { id =>
        val x: Future[Option[Tables.AlbumsType]] = ProductService.loadAlbum(id)
        val o = x.map { record =>
          record.fold(NotFound(id))(y => jsonOK(Album(y.id, y.title, y.publishDate, y.artistId).asJson))
        }.recover {
          case f =>
            log.error("err", f)
            InternalServerError("Error " + f)
        }
        Task.fromFuture(o).flatMap(a => a)
      }

    case req@POST -> Root / "rest" / "album" / idAlbum / idArtist =>
      log.debug(s"update album $idAlbum")

      upsertAlbum(req, idArtist, idAlbum)

    case req@POST -> Root / "rest" / "artist" / id =>
      log.debug(s"update artist $id")

      upsertArtist(req, id)

    case DELETE -> Root / "rest" / "album" / id / artistId =>
      log.debug(s"delete album $id $artistId")
      id.isUUID.fold(BadRequest(s"album id $id is not valid")) { id =>
        artistId.isUUID.fold(BadRequest(s"artist id $artistId is not valid"))(id => ProductService.deleteAlbum(id, id))
      }


    case DELETE -> Root / "rest" / "artist" / id =>
      log.debug(s"delete artist $id")
      id.isUUID.fold(BadRequest("id is not valid"))(ProductService.deleteArtist(_))

    case req@PUT -> Root / "rest" / "artist" / id =>
      log.debug(s"create artist $id")
      // PUT is idempotent
      val p: Future[Task[Response]] = ProductService.loadArtist(id).map { art =>
        art.fold(upsertArtist(req, id))(_ => Created("artist exists"))
      }
      Task.fromFuture(p).flatMap(a => a)

    case req@PUT -> Root / "rest" / "album" / idAlbum / idArtist =>
      log.debug(s"create artist $idAlbum")
      // PUT is idempotent
      val p = ProductService.loadAlbum(idAlbum).map { art =>
        art.fold(upsertAlbum(req, idArtist, idAlbum))(_ => Created("album exists"))
      }
      Task.fromFuture(p).flatMap(a => a)


    case GET -> Root / "rest" / "album" / "track" / name =>
      log.debug(s"received search album by track")
      val s1 = ProductService.searchTrack(name)
      s1.flatMap(a => jsonOK(a.asJson))

    case GET -> Root / "rest" / "artist" / "name" / name =>
      log.debug(s"received search artist by name")
      val s1 = ProductService.searchArtistByname(name)
      s1.flatMap(a => jsonOK(a.asJson))

    case GET -> Root / "rest" / "artist" / "random" / n =>
      val p = (0 until n.toInt).map(_ => ArtistPayload.random)
      jsonOK(p.asJson)

    case GET -> Root / "rest" / "album" / "random" / n =>
      val p = (0 until n.toInt).map(_ => AlbumPayload.random)
      jsonOK(p.asJson)

    case GET -> Root / "admin" / "check" =>
      log.debug("received admin check")
      val elastic = Properties.elasticSearch.check
      val kafka = Properties.kafka.fold(0)(c => c.check)
      val err = (if (!elastic) "ELASTIC SEARCH NOT RESPONDING" else "") + (if (kafka != 0) "\nKAFKA NOT RESPONDING" else "")

      val k = ProductService.loadAlbum(MyRandom.getRandomUUID.toString)
      val tr1 = k.map { _ =>
        ArtistPayload.random.asJson.hcursor.downField("name").as[String] match {
          case Right(_) =>
            if (elastic && kafka == 0) Ok("OK") else InternalServerError(err)
          case Left(f) =>
            log.error("Error $err", f)
            InternalServerError(s"$err\n$f")
        }
      }.recover {
        case f =>
          log.error("Error $err", f)
          InternalServerError(s"$err\n$f")
      }
      Task.fromFuture(tr1).flatMap(g => g)

  }

}
