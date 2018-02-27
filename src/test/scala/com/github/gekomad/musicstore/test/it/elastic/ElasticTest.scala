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

package com.github.gekomad.musicstore.test.it.elastic
/*
import com.github.gekomad.musicstore.model.json.elasticsearch.Products.ElasticArtist
import com.github.gekomad.musicstore.service.ElasticService
import com.github.gekomad.musicstore.utility.Net._
import com.github.gekomad.musicstore.utility.{MyRandom, Properties}
import com.whisk.docker.config.DockerKitConfig
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{DockerContainer, DockerKit}
import org.http4s._
import org.scalatest._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Failure

class ElasticTest   extends WordSpec with DockerKit
with Matchers with DockerKitConfig  with BeforeAndAfterEach
with DockerTestKit // scalatest integration
with DockerKitSpotify   with BeforeAndAfterAll {

val log: Logger = LoggerFactory.getLogger(this.getClass)

val elasticsearchContainer = configureDockerContainer("docker.elasticsearch")

override def dockerContainers: List[DockerContainer] = elasticsearchContainer :: super.dockerContainers

override def beforeAll(): Unit = {
  super.beforeAll()
  // if you want to write more integration test suites and run the migration only once, a different approach is needed
println
}

override def beforeEach(): Unit = {
  super.beforeEach()
  println
}

test("WriteRead") {

  val product: ElasticArtist = ElasticArtist.random
  val id = MyRandom.getRandomUUID.toString
  val index =  Properties.elasticSearch.index1

  val pp = ElasticService.insert(id, index, product).unsafeAttemptRun().toTry


  pp match {
    case Failure(f) =>
      log.error("err", f)
    case _ =>
  }
  assert(pp.isSuccess)

  val read = ElasticService.read(index, Properties.elasticSearch.artistType, id)

  read.map { r =>
    log.debug("read - return body: " + body(r))
    assert(r.status == Status.Ok)
  }

}


}*/
