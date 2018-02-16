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

package com.github.gekomad.musicstore.service

import com.github.gekomad.musicstore.model.json.elasticsearch.{ElasticProductBase, ElasticSearchTemplate}
import com.github.gekomad.musicstore.model.json.elasticsearch.Products.{ElasticAlbum, ElasticArtist}
import com.github.gekomad.musicstore.utility.Net._
import com.github.gekomad.musicstore.utility.Properties
import fs2.Task
import io.circe._
import io.circe.syntax._
import org.http4s.{Response, Uri}
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.slf4j.{Logger, LoggerFactory}
import io.circe.syntax._
import com.github.gekomad.musicstore.utility.MyPredef._
import java.time.LocalDate

import io.circe.Decoder.Result
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.syntax._
import java.time.LocalDate

import io.circe.Decoder.Result
import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.java8.time._

import scala.collection.immutable

object ElasticService {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  private val httpClient = PooledHttp1Client()

  implicit val musicIndex = Properties.elasticSearch.index1
  implicit val artistType = Properties.elasticSearch.artistType
  implicit val albumType = Properties.elasticSearch.albumType
  implicit val strategy3 = fs2.Strategy.fromFixedDaemonPool(10)

  def routing(index: String, theType: String, childId: String, parentId: String) = Properties.elasticSearch.host.withPath(s"""/$index/$theType/$childId?routing=$parentId""")

  def byId(index: String, theType: String, id: String) = Properties.elasticSearch.host.withPath(s"""/$index/$theType/$id""")

  def searchArtistByName(name: String)(index: String = musicIndex): Task[List[ElasticArtist]] = {
    val uri = Properties.elasticSearch.host.withPath(s"""/$index/$artistType/_search?q=name:$name""")

    val dd = httpClient.expect[String](uri)
    dd.map { a =>
      val o = parse(a).getOrElse(Json.Null)

      val p1 = o.as[ElasticSearchTemplate[ElasticArtist]]
      val p = p1.map { a =>
        a.hits.hits.map { z =>
          z._source
        }
      }
      p.getOrElse(throw new Exception(s"err decode $p"))
    }

  }

  def searchTrack(name: String)(index: String = musicIndex): Task[List[(String, ElasticAlbum)]] = {

    val uri = Properties.elasticSearch.host.withPath(s"""/$index/$albumType/_search?q=tracks:$name""")

    val dd = httpClient.expect[String](uri)
    dd.map { a =>
      val o = parse(a).getOrElse(Json.Null)

      val p1 = o.as[ElasticSearchTemplate[ElasticAlbum]]
      val p = p1.map { a =>
        a.hits.hits.map { z =>
          (z._id, z._source)
        }
      }
      p.getOrElse(throw new Exception("err decode"))
    }

  }

  def insert[A <: ElasticProductBase](id: String, index: String, product: A): Task[String] = {

    def artist(id: String, index: String, json: Json) = {
      val uriPut = byId(index, artistType, id)

      log.debug(s"putElastic: $uriPut $json")
      httpPut(uriPut, json)(httpClient) {
        case Ok(s) =>
          Task.now(body(s))
        case Created(s) =>
          Task.now(body(s))
        case e =>
          log.error("err ",e)
          Task.fail(new Exception(e.toString))
      }
    }

    def album(artistId: String, id: String, index: String, json: Json): Task[String] = {
      val uriPut = routing(index, albumType, id, artistId)
      log.debug(s"putElastic: $uriPut $json")
      httpPut(uriPut, json)(httpClient) {
        case Ok(s) =>
          Task.now(body(s))
        case Created(s) =>
          Task.now(body(s))
        case e =>
          log.error(s"$uriPut $json", e)
          Task.fail(new Exception(e.toString))
      }
    }

    product match {
      case a: ElasticArtist =>
        val json: Json = a.asJson
        artist(id, index, json)
      case a: ElasticAlbum =>
        val json: Json = a.asJson
        album(a.my_join_field.parent, id, index, json)
      case a => log.error(s"Product not found", a)
        Task.fail(new Exception(s"Product not found $a"))
    }

  }

  def read(index: String, theType: String, id: String): Task[Response] = httpGet(byId(index, theType, id))(httpClient)

  def albumsByArtist(index: String, id: String): Task[immutable.Seq[String]] = {
    val query =
      s""" {
      "query": {
        "parent_id": {
        "type": "album",
        "id": "$id"
      }
      },"_source": ""
    }"""

    import io.circe.parser.parse
    val queryJson: Json = parse(query).getOrElse(throw new Exception(s"err parse $query"))

    val uri = Properties.elasticSearch.host.withPath(s"""/$index/_search""")

    val p = httpPost(uri, queryJson)(httpClient) {
      case Ok(s) =>
        val a = body(s)
        val o = parse(a).getOrElse(Json.Null)

        val p1 = o.as[ElasticSearchTemplate[ElasticProductBase]]
        val p = p1.map { a =>
          a.hits.hits.map { z =>
            z._id
          }
        }
        val ps: immutable.Seq[String] = p.getOrElse(throw new Exception(s"err decode $p"))
        Task.now(ps)

      case e =>
        log.error("err", e)
        Task.fail(new Exception(e.toString))
    }
    p
  }

  def deleteArtistAndAlbums(index: String, theType: String, artistId: String): Task[Response] = {
    val albums = albumsByArtist(index, artistId)

    val o = albums.flatMap { albumList =>
      val o = albumList.map(albumId => deleteAlbum(index, theType, albumId, artistId))
      o.foldLeft(Task(Response(Ok)))((a, b) => a.flatMap(_ => b))
    }

    val uri = byId(index, theType, artistId)

    val v = httpDelete(uri)(httpClient)
    o.flatMap(_ => v)
  }


  def deleteAlbum(index: String, theType: String, albumId: String, artistId: String): Task[Response] = {
    log.debug("deleteAlbum")
    val uri = routing(index, theType, albumId, artistId)
    log.debug(uri.toString())
    httpDelete(uri)(httpClient)
  }

}
