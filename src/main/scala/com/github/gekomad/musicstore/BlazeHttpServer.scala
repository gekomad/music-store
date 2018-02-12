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

import com.github.gekomad.musicstore.utility.Properties
import fs2.{Stream, Task}
import org.http4s.server.blaze._
import org.http4s.util.StreamApp
import org.slf4j.{Logger, LoggerFactory}
import scala.util.Properties.envOrNone

object BlazeHttpServer extends StreamApp {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def stream(args: List[String]): Stream[Task, Nothing] =
    BlazeBuilder
      .bindHttp(Properties.httpPort, Properties.host)
      .mountService(Route.service)
      .serve

  StartupServices

}
