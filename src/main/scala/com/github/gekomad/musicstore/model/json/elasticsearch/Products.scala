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

package com.github.gekomad.musicstore.model.json.elasticsearch

import java.time.LocalDate
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.utility.MyRandom._

class ElasticProductBase

object Products {

  final case class ElasticArtist(
                                  name: String,
                                  genres: List[String],
                                  origin: String,
                                  year: Int,
                                  members: List[String],
                                  url: Option[String],
                                  activity: Boolean,
                                  description: Option[String],
                                  my_join_field: String = "artist"
                                ) extends ElasticProductBase

  object ElasticArtist {

    def apply(artist: ArtistPayload): ElasticArtist =
      new ElasticArtist(artist.name, artist.genres, artist.origin, artist.year, artist.members, artist.url, artist.activity, artist.description)

    def random: ElasticArtist = {
      val name = getRandomString(10)
      val genres = getRandomStringList(3, 8)
      val origin = getRandomString(10)
      val year = getRandomInt(1900, 2018)
      val members = getRandomStringList(3, 8)
      val url = getRandomUrl
      val activity = getRandomBoolean
      val description = getRandomBigString()
      ElasticArtist(name, genres, origin, year, members, Some(url), activity, Some(description))
    }
  }

  final case class MyJoinField(name: String, parent: String)


  final case class ElasticAlbum(
                                 title: String,
                                 publishDate: LocalDate,
                                 length: Int,
                                 tracks: List[String],
                                 my_join_field: MyJoinField
                               ) extends ElasticProductBase

  object ElasticAlbum {

    def apply(idArtist: String, album: AlbumPayload): ElasticAlbum =
      new ElasticAlbum( album.title, album.publishDate, album.length, album.tracks, MyJoinField("album", idArtist))


    def random: ElasticAlbum = {
      val title = getRandomString(10)
      val publishDate = getRandomLocalDate
      val length = getRandomInt(100, 2000)
      val tracks = getRandomStringList(3, 10)
      val idArtist = getRandomUUID.toString
      ElasticAlbum( title, publishDate, length, tracks, MyJoinField("album", idArtist))
    }

  }

}
