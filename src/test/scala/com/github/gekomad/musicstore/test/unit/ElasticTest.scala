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

package com.github.gekomad.musicstore.test.unit

import com.github.gekomad.musicstore.model.json.elasticsearch.Products.ElasticArtist
import com.github.gekomad.musicstore.service.ElasticService
import com.github.gekomad.musicstore.utility.Net._
import com.github.gekomad.musicstore.utility.{MyRandom, Properties}
import fs2.Task
import io.circe.Json
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.scalatest._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.java8.time._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

class ElasticTest extends FunSuite {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  test("WriteRead") {

    val product: ElasticArtist = ElasticArtist.random
    val id = MyRandom.getRandomUUID.toString
    val index = "test_index"

    val p = ElasticService.insert(id, index, product)
    p.map { x =>
      println("put - return body: " + x)
    }

    val pp = p.unsafeAttemptRun().toTry
    pp match {
      case Failure(f) => log.error("err", f)
      case _ =>
    }
    assert(pp.isSuccess)

    val read = ElasticService.read(index, Properties.elasticSearch.artistType, id)

    read.map { r =>
      log.debug("read - return body: " + body(r))
      assert(r.status == Status.Ok)
    }

  }

}
