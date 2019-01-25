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

import cats.data.Kleisli
import cats.effect.IO
import cats.effect._
import com.github.gekomad.musicstore.utility.Properties
import org.http4s.{Request, Response}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.syntax._
import org.http4s.dsl.io._
import org.http4s.server.blaze._

object BlazeServer extends IOApp  {

  def run(args: List[String]): IO[ExitCode] =
  BlazeServerBuilder[IO]
    .bindHttp(Properties.httpPort, Properties.host) //.mountService(Route.service, "/")
    .withHttpApp(Route.service)
    .serve
    .compile
    .drain
    .as(ExitCode.Success)

}
