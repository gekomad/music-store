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

import java.sql.Timestamp
import java.time.LocalDate

import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.service.{Err, MyErrors}
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.Utility._
import io.circe.parser._
import io.circe.{Decoder, HCursor, Json}
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.switch
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.parser.parse
import com.github.gekomad.musicstore.utility.UUIDable._
import com.github.gekomad.musicstore.utility.UUIDableInstances._
import io.circe.Decoder.Result

object Validator {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def fieldIsValid[A: Decoder](cursor: HCursor, field: String, f: A => Boolean, optional: Boolean = false): Validated[NonEmptyList[Err], A] = {

    val node = cursor.downField(field).as[A]

    (node: @switch) match {
      case Left(ee) =>
        Validated.invalidNel(Err(s"${ee.message}", field, MyErrors.DecodeJsonError))
      case Right(gg) =>
        if (f(gg))
          Validated.valid(gg)
        else
          Validated.invalidNel(Err(s"$field is malformed", gg.toString, MyErrors.InsertError))
    }
  }

  def validateId(id: String): Validated[NonEmptyList[Err], String] = id.isUUID.map(Validated.valid(_)).getOrElse {
    Validated.invalidNel(Err(s"ID is malformed", id, MyErrors.InsertError))
  }

  //  def checkAlbums(o: List[Album]) = {
  //
  //    val oo = o.map { x =>
  //      val p = if (x.duration <= 0) Validated.invalidNel(Err(s"duration is not valid", "duration", MyErrors.InsertError)) else
  //        Validated.valid("duration")
  //      p
  //    }
  //
  //
  //    val pp = oo.reduce((a, b) => a |@| b)
  //    pp
  //  }

  def validateArtist(json: String): Validated[NonEmptyList[Err], (String, String)] = {
    log.debug(s"validateArtist: $json")

    val doc: Json = parse(json).getOrElse(Json.Null)
    val cursor: HCursor = doc.hcursor

    val checkName = fieldIsValid[String](cursor, "name", !isBlank(_))
    val checkUrl = fieldIsValid[String](cursor, "url", isDomain, optional = true)
    // val checkId: Validated[NonEmptyList[Err], String] = fieldIsValid(cursor, "ishDate, album.code, album.artistId.toStrid", _.isUUID)

    val l = checkName |@| checkUrl // |@| checkId //|@| checkAlbum

    l.tupled

  }

  def validateAlbum(json: String): Validated[NonEmptyList[Err], (String, Int)] = {
    log.debug(s"validateAlbum: $json")

    val doc: Json = parse(json).getOrElse(Json.Null)
    val cursor: HCursor = doc.hcursor

    val checkTitle = fieldIsValid[String](cursor, "title", !isBlank(_))
    val checkDuration = fieldIsValid[Int](cursor, "duration", _ != 0)

    val l = checkTitle |@| checkDuration
    l.tupled
  }

}
