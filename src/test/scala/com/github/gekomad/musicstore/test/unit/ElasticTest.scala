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

import cats.effect.IO
import com.github.gekomad.musicstore.model.json.elasticsearch.Products.ElasticArtist
import com.github.gekomad.musicstore.service.ElasticService
import com.github.gekomad.musicstore.utility.{MyRandom, Properties}
import io.circe.java8.time._
import org.http4s._
import org.http4s.client.blaze.Http1Client
import org.scalatest._
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

class ElasticTest extends FunSuite with BeforeAndAfterAll {

  val log: Logger = LoggerFactory.getLogger(this.getClass)


  override def beforeAll(): Unit = {
    if (!Properties.elasticSearch.check)
      fail("ELASTICSEARCH IS NOT RESPONDING. DID YOU RUN test.sh ?")
    else {
      val o = ElasticService.createSchema
      Try(Await.result(o.unsafeToFuture(), Duration.Inf)) match {
        case Failure(f) => if (f.getMessage.contains("resource_already_exists_exception")) () else throw new Exception(f)
        case _ =>
      }
    }
  }

  test("WriteRead") {

    val product: ElasticArtist = ElasticArtist.random
    val id = MyRandom.getRandomUUID.toString
    val index = Properties.elasticSearch.index1

    val pp = ElasticService.insert(id, index, product)
    val httpClient = Http1Client[IO]().unsafeRunSync

    pp.flatMap { r =>
      val read = ElasticService.read(index, Properties.elasticSearch.artistType, id)
      httpClient.fetch(read) {
        x =>
          x.status match {
            case Status.Ok =>
              IO(1)
            case e =>
              fail(e.toString)
              IO(1)
          }
      }

    }

  }
}
