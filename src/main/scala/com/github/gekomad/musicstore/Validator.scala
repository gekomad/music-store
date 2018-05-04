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

import java.time.LocalDate
import cats.data.Validated
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.service.{Err, MyErrors}
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.Utility._
import io.circe.{Decoder, HCursor, Json}
import org.slf4j.{Logger, LoggerFactory}
import io.circe.java8.time._
import io.circe.parser.parse
import com.github.gekomad.musicstore.utility.UUIDable._
import com.github.gekomad.musicstore.utility.UUIDableInstances._
import cats.instances.all._
import cats.syntax.apply._
import cats.syntax.either._

object Validator {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  type FailSlow[A] = Validated[List[Err], A]

  def fieldIsValid[A: Decoder](cursor: HCursor, field: String)(check: A => Boolean = (_: A) => true): Either[List[Err], A] = {

    cursor.downField(field).as[A] match {
      case Left(ee) =>
        Left(List(Err(s"${ee.message}", field, MyErrors.DecodeJsonError)))
      case Right(gg) =>
        if (check(gg))
          Right(gg)
        else
          Left(List(Err(s"field '$field' is malformed", gg.toString, MyErrors.InsertError)))
    }
  }

  def validateId(id: String): Either[List[Err], String] = id.isUUID.map(Right(_)).getOrElse {
    Left(List(Err(s"ID is malformed", id, MyErrors.InsertError)))
  }

  def validateArtist(json: String): FailSlow[ArtistPayload] = {
    log.debug(s"validateArtist: $json")

    def isDomainOption(o: Option[String]): Boolean = o.fold(true)(isDomain(_))

    val doc: Json = parse(json).getOrElse(Json.Null)
    val cursor: HCursor = doc.hcursor

    val name = fieldIsValid[String](cursor, "name")(!isBlank(_))
    val genres = fieldIsValid[List[String]](cursor, "genres")()
    val origin = fieldIsValid[String](cursor, "origin")()

    val year = fieldIsValid[Int](cursor, "year")()
    val members = fieldIsValid[List[String]](cursor, "members")()
    val url = fieldIsValid[Option[String]](cursor, "url")(isDomainOption(_))
    val activity = fieldIsValid[Boolean](cursor, "activity")()
    val description = fieldIsValid[Option[String]](cursor, "description")()


    val artist = (
      name.toValidated,
      genres.toValidated,
      origin.toValidated,
      year.toValidated,
      members.toValidated,
      url.toValidated,
      activity.toValidated,
      description.toValidated
    ).mapN(ArtistPayload.apply)

    artist
  }

  def validateAlbum(json: String): FailSlow[AlbumPayload] = {
    log.debug(s"validateAlbum: $json")

    val doc: Json = parse(json).getOrElse(Json.Null)
    val cursor: HCursor = doc.hcursor

    val title = fieldIsValid[String](cursor, "title")(!isBlank(_))
    val publishDate = fieldIsValid[LocalDate](cursor, "publishDate")()
    val length = fieldIsValid[Int](cursor, "length")(_ != 0)
    val price = fieldIsValid[Float](cursor, "price")()
    val tracks = fieldIsValid[List[String]](cursor, "tracks")()
    val quantity = fieldIsValid[Int](cursor, "quantity")()
    val discount = fieldIsValid[Float](cursor, "discount")()
    val seller = fieldIsValid[String](cursor, "seller")()
    val code = fieldIsValid[String](cursor, "code")()

    val album = (
      title.toValidated,
      publishDate.toValidated,
      length.toValidated,
      price.toValidated,
      tracks.toValidated,
      quantity.toValidated,
      discount.toValidated,
      seller.toValidated,
      code.toValidated
    ).mapN(AlbumPayload.apply)

    album

  }

}
