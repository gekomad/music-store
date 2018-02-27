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

package com.github.gekomad.musicstore.test.it

import com.github.gekomad.musicstore.utility.Net._
import fs2.Task
import io.circe.Json
import io.circe.parser.decode
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.utility.Properties
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze._
import org.http4s.client.Client
import org.http4s.dsl._
import org.slf4j.{Logger, LoggerFactory}
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import org.scalatest._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.java8.time._
import scala.util.Try

object Common {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  private val httpClient: Client = PooledHttp1Client()

  private val h = if (Properties.host.contains("://")) Properties.host else "http://" + Properties.host
  private val x = Uri.fromString(s"$h:" + Properties.httpPort)
  require(x.isRight)
  val TEST_SERVER_URL: Uri = x.getOrElse(throw new Exception)

  val RANDOM_ARTIST_PATH: String = "/rest/artist/random/1"
  val ARTIST_PATH: String = "/rest/artist"
  val ALBUM_PATH: String = "/rest/album"

  def createSchema: Task[Response] = {
    val uri = TEST_SERVER_URL / "rest" / "create_sql_schema"
    val resp: Task[Response] = httpGet( uri)(httpClient)
    resp
  }

}
