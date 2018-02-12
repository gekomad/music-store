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

package com.github.gekomad.musicstore.model.json.in

import java.time.LocalDate
import com.github.gekomad.musicstore.utility.MyRandom._
import io.circe.Decoder.Result
import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.java8.time._
import scala.util.Try

final case class AlbumPayload(
                               title: String,
                               publishDate: LocalDate,
                               duration: Int,
                               price: Float,
                               tracks: List[String],
                               quantity: Int,
                               discount: Float,
                               seller: String,
                               code: String) extends ProductBase

object AlbumPayload {
  def apply(json: String): Try[AlbumPayload] = parse(json).toTry.map(_.as[AlbumPayload].toTry).flatten

  def random: AlbumPayload = AlbumPayload(
    getRandomString(10),
    getRandomLocalDate,
    getRandomInt(200, 4000),
    getRandomFloat * 100,
    getRandomStringList(10, 8),
    getRandomInt(100),
    getRandomFloat,
    getRandomString(10),
    getRandomString(5)
  )
}

trait ProductBase

object ProductBase {

  final case class ArtistPayload(
                                  name: String,
                                  genres: List[String],
                                  origin: String,
                                  year: Int,
                                  members: List[String],
                                  url: Option[String],
                                  activity: Boolean,
                                  description: Option[String]
                                ) extends ProductBase

  object ArtistPayload {

    def apply(json: String): Try[ArtistPayload] = parse(json).toTry.map(_.as[ArtistPayload].toTry).flatten

    def random: ArtistPayload = {
      val name = getRandomString(10)
      val genres = getRandomStringList(3, 8)
      val origin = getRandomString(10)
      val year = getRandomInt(1900, 2018)
      val members = getRandomStringList(3, 8)
      val url = getRandomUrl
      val activity = getRandomBoolean
      val description = getRandomBigString()
      ArtistPayload(name, genres, origin, year, members, Some(url), activity, Some(description))
    }

  }

}
