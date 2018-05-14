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

import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.java8.time._
import cats.data.Validated.{Invalid, Valid}
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import io.circe._
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.model.json.out.{Album, Artist}
import com.github.gekomad.musicstore.model.sql.Tables
import com.github.gekomad.musicstore.service._
import org.http4s.circe._
import org.slf4j.{Logger, LoggerFactory}
import com.github.gekomad.musicstore.utility.{MyRandom, Properties}
import com.github.gekomad.musicstore.utility.MyPredef._
import cats.effect._
import com.github.gekomad.musicstore.service.SqlService.log
import org.http4s._
import org.http4s.dsl.io._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.gekomad.musicstore.utility.UUIDable._
import com.github.gekomad.musicstore.utility.UUIDableInstances._
import scala.util.{Failure, Success, Try}

object Route {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def removeNull(x: Json): String = x.pretty(Printer.spaces2.copy(dropNullValues = true))

  def jsonOK(s1: Json): IO[Response[IO]] = Ok(removeNull(s1)).map(_.putHeaders(Header("Content-Type", "application/json")))

  private def upsertAlbum(req: Request[IO], idArtist: String, idAlbum: String): IO[Response[IO]] = req.as[String].flatMap { json =>
    Validator.validateAlbum(json) match {
      case Invalid(ii) =>
        val ll = ii.map { x =>
          val p = Err(x.desc, x.value, x.errorType)
          log.error(s"invalid $p")
          p
        }
        BadRequest(ll.asJson)
      case _ =>
        ProductService.upsertAlbum(idArtist, idAlbum, json).attempt.flatMap {
          _.toTry match {
            case Failure(f) => log.error("Error", f)
              BadRequest("Error " + f)
            case Success(_) => Created()
          }
        }
    }
  }


  private def upsertArtist(req: Request[IO], id: String): IO[Response[IO]] = req.as[String].flatMap { json =>
    val x = Validator.validateArtist(json)
    x match {
      case Invalid(ii) =>
        val ll = ii.map { x =>
          val p = Err(x.desc, x.value, x.errorType)
          log.error(s"invalid $p")
          p
        }
        BadRequest(ll.asJson)
      case Valid(artistPayload) =>
        ProductService.upsertArtist(id, artistPayload, json).attempt.flatMap {
          _.toTry match {
            case Failure(f) => log.error("Error", f)
              BadRequest("Error " + f)
            case Success(_) => Created()
          }
        }
    }
  }


  def createSqlSchema: IO[Response[IO]] = {
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
    val p = IO.fromFuture(IO(o)).flatMap(a => a)
    p
  }

  val service: HttpService[IO] = HttpService[IO] {

    case GET -> Root / "rest" / "create_sql_schema" =>
      log.debug(s"received create_sql_schema")
      createSqlSchema

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

        IO.fromFuture(IO(o)).flatMap(a => a)
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
        IO.fromFuture(IO(o)).flatMap(a => a)
      }

    case req@POST -> Root / "rest" / "album" / idAlbum / idArtist =>
      log.debug(s"update album $idAlbum")
      upsertAlbum(req, idArtist, idAlbum)

    case req@POST -> Root / "rest" / "artist" / id =>
      log.debug(s"update artist $id")
      upsertArtist(req, id)

    case DELETE -> Root / "rest" / "album" / id / artistId =>
      log.debug(s"delete album $id $artistId")
      id.isUUID.fold(BadRequest(s"album id $id is not valid")) { _ =>
        artistId.isUUID.fold(BadRequest(s"artist id $artistId is not valid")) { id =>
          ProductService.deleteAlbum(id, id).attempt.flatMap {
            _.toTry match {
              case Failure(_) => BadRequest(s"album $id artist $artistId is not valid")
              case Success(_) => Ok(id)
            }
          }
        }
      }

    case DELETE -> Root / "rest" / "artist" / id =>
      log.debug(s"delete artist $id")
      id.isUUID.fold(BadRequest("id is not valid")) { i =>
        ProductService.deleteArtist(i).attempt.flatMap {
          _.toTry match {
            case Failure(_) => BadRequest(s"artist id $id is not valid")
            case Success(_) => Ok(id)
          }
        }
      }

    case req@PUT -> Root / "rest" / "artist" / id =>
      log.debug(s"create artist $id")
      // PUT is idempotent
      val p = ProductService.loadArtist(id).map { art =>
        art.fold(upsertArtist(req, id))(_ => Created("artist exists"))
      }
      IO.fromFuture(IO(p)).flatMap(a => a)

    case req@PUT -> Root / "rest" / "album" / idAlbum / idArtist =>
      log.debug(s"create artist $idAlbum")
      // PUT is idempotent
      val p = ProductService.loadAlbum(idAlbum).map { art =>
        art.fold(upsertAlbum(req, idArtist, idAlbum))(_ => Created("album exists"))
      }
      IO.fromFuture(IO(p)).flatMap(a => a)


    case GET -> Root / "rest" / "album" / "track" / name =>
      log.debug(s"received search album by track")
      val s1 = ProductService.searchTrack(name)
      s1.flatMap(a => jsonOK(a.asJson))

    //Get albums avg length by artist_id
    case GET -> Root / "rest" / "aggregations" / "artist" / "album" / "length" / "avg" / artistId =>
      log.debug(s"received get albums avg length for $artistId")
      val s1 = ProductService.albumsAvgDuration(artistId)
      s1.flatMap(a => jsonOK(a))

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
      val err = (if (!elastic) "ELASTIC SEARCH IS NOT RESPONDING" else "") + (if (kafka != 0) "\nKAFKA IS NOT RESPONDING" else "")

      val k = ProductService.artistsCount
      val tr1 = k.map { count =>
        ArtistPayload.random.asJson.hcursor.downField("name").as[String] match {
          case Right(_) =>
            if (elastic && kafka == 0) Ok(s"artist count: $count") else InternalServerError(err)
          case Left(f) =>
            log.error("Error $err", f)
            InternalServerError(s"$err\n$f")
        }
      }
        .recover {
          case f =>
            log.error("Error $err", f)
            InternalServerError(s"$err\n$f")
        }
      IO.fromFuture(IO(tr1)).flatMap(g => g)

  }

}
