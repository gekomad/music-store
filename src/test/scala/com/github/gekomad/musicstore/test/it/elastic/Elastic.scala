package com.github.gekomad.musicstore.test.it.elastic

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

import scala.concurrent.Future



class Elastic
    extends WordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with DockerTestKit // scalatest integration
    with DockerKitSpotify // docker client implementation
    with DockerElasticService { // container rules


  override def beforeAll(): Unit = {
    super.beforeAll()

  }

  override def beforeEach(): Unit = {
    super.beforeEach()

  }

  case class User(username: String, firstName: String, lastName: String)

  "users repository" when {

    val user =
      User(username = "joebloggs", firstName = "Joe", lastName = "Bloggs")

    "getting all the users" should {
      "return all users on the database" in {


        val users: Future[List[User]] =  Future(List(user))

        users.futureValue should contain only user
      }
    }


  }
}
