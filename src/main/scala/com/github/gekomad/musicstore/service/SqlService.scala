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

package com.github.gekomad.musicstore.service

import com.github.gekomad.musicstore.Route.log
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.model.sql.Tables
import com.github.gekomad.musicstore.model.sql.Tables.{AlbumsType, ArtistsType}
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.Properties.sql.dc._
import com.github.gekomad.musicstore.utility.Properties.sql.dc.profile.api._
import org.http4s.dsl.io.{InternalServerError, Ok}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

object SqlService {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def upsertArtist(id: String, artist: ArtistPayload): Future[Int] = {
    log.debug(s"upsert artist $id")

    val writeArtist = ArtistDAO.upsert(ArtistsType(
      id,
      now,
      artist.name,
      artist.url,
      artist.activity
    )
    )

    db.run(writeArtist)
  }

  def upsertAlbum(id: String, idArtist: String, album: AlbumPayload): Future[Int] = {
    log.debug(s"upsert album $id")

    val at = AlbumsType(id, album.title, now, album.publishDate, album.code, idArtist)
    val writeAlbum = AlbumDAO.upsert(at)
    db.run(writeAlbum.transactionally)
  }

}
