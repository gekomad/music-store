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

package com.github.gekomad.musicstore.utility

import java.net.Socket
import io.circe.Json
import org.slf4j.{Logger, LoggerFactory}
import scala.util.{Failure, Success, Try}

object Net {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  import cats.effect._
  import org.http4s._

  implicit def defaultHeader: Header = Header("Content-Type", "application/json")

  import org.http4s.dsl.io._

  def httpGet[A](uri: Uri): Request[IO] = Request[IO](Method.GET, uri = uri)

  def httpDelete[A](uri: Uri): Request[IO] = Request[IO](Method.DELETE, uri = uri)

  import cats.effect.IO

  import org.http4s.{Method, Request, Uri}
  import org.http4s.circe._

  def httpPut[A](uri: Uri, body: Json)(implicit h: Header): IO[Request[IO]] = Request[IO](Method.PUT, uri = uri).withBody(body).
    map(_.putHeaders(h))

  def httpPost[A](uri: Uri, body: Json)(implicit h: Header): IO[Request[IO]] = Request[IO](Method.POST, uri = uri).withBody(body).
    map(_.putHeaders(h))

  def body(res: Response[IO]): IO[String] = res.body.compile.toVector.map(_.map(_.toChar).mkString)

  def serverListening(host: String, port: Int): Boolean = Try {
    new Socket(host, port)
  }.map(_.close) match {
    case Success(_) => true
    case Failure(_) => false
  }

}
