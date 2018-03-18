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


import com.github.gekomad.musicstore.utility.Net._
import org.slf4j.{Logger, LoggerFactory}
import com.github.gekomad.musicstore.utility.Properties
import org.http4s._


object Common {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val h = if (Properties.host.contains("://")) Properties.host else "http://" + Properties.host

  val TEST_SERVER_URL: Uri = Uri.fromString(s"$h:" + Properties.httpPort).getOrElse(throw new Exception)

  val RANDOM_ARTIST_PATH: String = "/rest/artist/random/1"
  val ARTIST_PATH: String = "/rest/artist"
  val ALBUM_PATH: String = "/rest/album"

  def createSchema = httpGet(TEST_SERVER_URL / "rest" / "create_sql_schema")


}
