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

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.gekomad.musicstore.model.sql.Tables.{AlbumsEntity, ArtistsEntity, BaseEntity}
import com.github.gekomad.musicstore.service.AlbumDAO.theTable
import com.github.gekomad.musicstore.utility.Properties
import org.slf4j.{Logger, LoggerFactory}
import slick.jdbc.JdbcBackend
import slick.lifted.{AbstractTable, TableQuery}

import scala.concurrent.Future

abstract class GenericDAO[TheEntity <: AbstractTable[_] with BaseEntity](val theTable: TableQuery[TheEntity]) {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  import Properties.sql.dc.profile.api._

  val db: JdbcBackend#DatabaseDef = Properties.sql.dc.db

  def count: Future[Int] = {
    import Properties.sql.dc.profile.api._
    val query = theTable.size.result
    log.debug("Generated SQL for filter query: {}", query.statements.head)
    db.run(query)
  }

  def upsert(value: TheEntity#TableElementType) = {
    log.debug(s"Insert in table \nvalues: $value")
    val g = theTable.insertOrUpdate(value)
    val query: String = g.statements.head
    log.debug(s"Insert in table\nquery: $query \nvalues: $value")
    g
  }

  /*
    load a record by key
   */

  def load(key: String): Future[Option[TheEntity#TableElementType]] = {
    import Properties.sql.dc.profile.api._
    val query = theTable.filter(_.id === key).result.headOption
    log.debug("Generated SQL for filter query: {}", query.statements.head)
    db.run(query)
  }

  /*
    delete a record by key
  */
  def delete(key: String): Future[Int]

}

object ArtistDAO extends GenericDAO[ArtistsEntity](TableQuery[ArtistsEntity]) {
  def delete(key: String): Future[Int] = {
    import Properties.sql.dc.profile.api._
    val action = theTable.filter(_.id === key).delete
    val affectedRowsCount: Future[Int] = db.run(action)
    affectedRowsCount
  }
}

object AlbumDAO extends GenericDAO[AlbumsEntity](TableQuery[AlbumsEntity]) {

  def delete(key: String): Future[Int] = {
    import Properties.sql.dc.profile.api._
    val action = theTable.filter(_.id === key)
    val affectedRowsCount: Future[Int] = db.run(action.delete)
    affectedRowsCount
  }

  def artistIdByAlbumId(albumId: String): Future[Option[String]] = {
    import Properties.sql.dc.profile.api._
    val query = theTable.filter(_.id === albumId).result.headOption
    db.run(query).map(a => a.map(b => b.artistId))
  }
}
