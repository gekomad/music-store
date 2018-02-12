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

package com.github.gekomad.musicstore.model.sql

import java.sql.Timestamp

import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.MyRandom.{getRandomBoolean, getRandomInt, getRandomString, getRandomUrl, _}
import com.github.gekomad.musicstore.utility.Properties
import org.slf4j.{Logger, LoggerFactory}
import slick.jdbc.meta.MTable
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Tables {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  import Properties.dc.profile.api._

  val allTables = List(artists, albums)
  val allTablesSchema = allTables.map(_.schema)

  def createSchema = {
    val dc = Properties.dc
    val c = (artists.schema ++ albums.schema).create
    for {
      k <- dc.db.run(c).map(_ => Success("ok")) recover {
        case e: Exception =>
          log.warn("create schema something went wrong " + e)
          Success("ok")
      }
    } yield k
  }

  def loadSchema = {
    val dc = Properties.dc
    val o = for {
      oo <- dc.db.run(MTable.getTables("%")) recover {
        case e: Exception =>
          log.error("getTables something went wrong " + e)
          Vector.empty[MTable]
      }
      if allTables.map(_.shaped.value.tableName).forall(oo.map(x => x.name.name).toList.contains)
    } yield Success(oo)

    o recover {
      case e: Exception =>
        log.error("getAlbum something went wrong " + e)
        Failure(e)
    }
  }

  /*
  BaseEntity
   */
  trait BaseEntity {
    self: Table[_] =>

    def id = column[String]("ID", O.Length(36))

    def dtInsert = column[Timestamp]("DT_INSERT", SqlType("TIMESTAMP NOT NULL default CURRENT_TIMESTAMP"))

  }

  final case class ArtistsType(id: String, dtInsert: Timestamp, name: String, url: Option[String], activity: Boolean)

  object ArtistsType {
    val tupled = (this.apply _).tupled

    def getRandom = new ArtistsType(
      getRandomUUID.toString, now, getRandomString(10), Some(getRandomUrl), getRandomBoolean
    )
  }


  class ArtistsEntity(tag: Tag)
    extends Table[ArtistsType](tag, "ARTISTS") with BaseEntity {

    def name = column[String]("NAME", O.Length(150))

    def url = column[Option[String]]("URL", O.Length(1000))

    def activity = column[Boolean]("ACTIVITY")

    def * = (id, dtInsert, name, url, activity).mapTo[ArtistsType]

    def pk = primaryKey("pk_artists", id)

  }

  lazy val artists = TableQuery[ArtistsEntity]

  final case class AlbumsType(id: String, title: String, dt_insert: Timestamp, publishDate: Timestamp, code: String, artistId: String)

  object AlbumsType {
    val tupled = (this.apply _).tupled

    def getRandom =
      new AlbumsType(
        getRandomUUID.toString,
        getRandomString(10),
        now,
        getRandomTimestamp,
        getRandomString(5),
        getRandomUUID.toString
      )
  }

  class AlbumsEntity(tag: Tag)
    extends Table[AlbumsType](tag, "ALBUMS") with BaseEntity {

    def publishDate = column[Timestamp]("PUBLISH_DATE", SqlType("TIMESTAMP default CURRENT_TIMESTAMP"))

    def code = column[String]("CODE")

    def title = column[String]("TITLE")

    def artistId = column[String]("ARTIST_ID", O.Length(36))

    def * = (id, title,dtInsert, publishDate, code, artistId).mapTo[AlbumsType]

    def pk = primaryKey("pk_albums", id)

    def artistfk = foreignKey("fk_artist", artistId, artists)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  }

  lazy val albums = TableQuery[AlbumsEntity]


}
