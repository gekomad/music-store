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

import fs2.Task
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

object Net {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def httpGet(uri: Uri)(httpClient: Client): Task[Response] = httpClient.get(uri)(Task.now)

  def httpDelete[A](uri: Uri)(httpClient: Client): Task[Response] =
    httpClient.fetch(Request(method = Method.DELETE, uri = uri))(Task.now)

  def httpPut[A](uri: Uri, body: Json)(httpClient: Client, h: Header = Header("Content-Type", "application/json")): (Response => Task[A]) => Task[A] =
    httpClient.fetch(Request(method = Method.PUT, uri = uri).putHeaders(h).withBody(body))

  def httpPost[A](uri: Uri, body: Json)(httpClient: Client, h: Header = Header("Content-Type", "application/json")): (Response => Task[A]) => Task[A] =
    httpClient.fetch(Request(method = Method.POST, uri = uri).putHeaders(h).withBody(body))

  def body(s: Response): String =
    new String(s.body.runLog.unsafeRun.foldLeft(scodec.bits.ByteVector.empty)(_ :+ _).toArray)


  def serverListening(host: String, port: Int): Boolean = {
    val p = Try {
      new Socket(host, port)
    }.map(_.close)

    p match {
      case Success(_) => true
      case Failure(_) => false
    }
  }
}
