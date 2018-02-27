package com.github.gekomad.musicstore.test.it.elastic

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait DockerElasticService extends DockerKit {

  import scala.concurrent.duration._

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300

  lazy val elasticsearchContainer = DockerContainer("elasticsearch:1.7.1")
    .withPorts(DefaultElasticsearchHttpPort -> Some(9200), DefaultElasticsearchClientPort -> Some(9300))
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultElasticsearchHttpPort, "/")
        .within(100.millis)
        .looped(20, 1250.millis)
    )

//  lazy val zookeeperContainer = DockerContainer("jplock/zookeeper:3.4.6")
//    .withPorts(2181 -> Some(2181))
//    .withReadyChecker(
//      DockerReadyChecker
//        .HttpResponseCode(2181, "/")
//        .within(100.millis)
//        .looped(20, 1250.millis)
//    )


  abstract override def dockerContainers: List[DockerContainer] =
//    zookeeperContainer :: (elasticsearchContainer :: super.dockerContainers)
    elasticsearchContainer :: super.dockerContainers

}
