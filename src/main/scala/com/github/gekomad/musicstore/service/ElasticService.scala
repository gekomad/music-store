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

import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.java8.time._
import com.github.gekomad.musicstore.model.json.elasticsearch.{ElasticProductBase, ElasticSearchTemplate}
import com.github.gekomad.musicstore.model.json.elasticsearch.Products.{ElasticAlbum, ElasticArtist}
import com.github.gekomad.musicstore.utility.Net.{body, _}
import com.github.gekomad.musicstore.utility.Properties
import io.circe._
import io.circe.syntax._
import org.http4s.{Request, Status, Uri}
import org.slf4j.{Logger, LoggerFactory}
import cats.effect.IO
import org.http4s.client.Client
import org.http4s.client.blaze._
import scala.collection.immutable

object ElasticService {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  private val httpClient: Client[IO] = Http1Client[IO]().unsafeRunSync

  private val artistType: String = Properties.elasticSearch.artistType
  private val albumType: String = Properties.elasticSearch.albumType

  def routing(index: String, theType: String, childId: String, parentId: String): Uri = Properties.elasticSearch.host.withPath(s"""/$index/$theType/$childId?routing=$parentId""")

  def byId(index: String, theType: String, id: String): Uri = Properties.elasticSearch.host.withPath(s"""/$index/$theType/$id""")

  def searchArtistByName(name: String)(index: String = Properties.elasticSearch.index1) = {
    val uri = Properties.elasticSearch.host.withPath(s"""/$index/$artistType/_search?q=name:$name""")

    httpClient.expect[String](uri).map { a =>
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

  def searchTrack(name: String)(implicit index: String = Properties.elasticSearch.index1) = {

    val uri = Properties.elasticSearch.host.withPath(s"""/$index/$albumType/_search?q=tracks:$name""")

    httpClient.expect[String](uri).map { a =>
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

  def insert[A <: ElasticProductBase](id: String, index: String, product: A): IO[String] = {

    def artist(id: String, index: String, json: Json): IO[String] = {
      val uriPut = byId(index, artistType, id)

      log.debug(s"putElastic: $uriPut $json")
      val s = httpPut(uriPut, json)

      httpClient.fetch(s) {
        x =>
          x.status match {
            case Status.Ok | Status.Created =>
              body(x)
            case e =>
              log.error(s"putElastic artist err $uriPut $e")
              IO.raiseError(new Exception(e.toString))
          }
      }
    }

    def album(artistId: String, id: String, index: String, json: Json): IO[String] = {
      val uriPut = routing(index, albumType, id, artistId)
      log.debug(s"putElastic: $uriPut $json")

      httpClient.fetch(httpPut(uriPut, json)) {
        x =>
          x.status match {
            case Status.Ok | Status.Created =>
              body(x)
            case e =>
              log.error(s"err $uriPut $e")
              IO.raiseError(new Exception(e.toString))
          }
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
        IO.raiseError(new Exception(s"Product not found $a"))
    }

  }

  def read(index: String, theType: String, id: String) = httpGet(byId(index, theType, id))

  def albumsByArtist(index: String, id: String) = {
    val query =
      s""" {
      "query": {
        "parent_id": {
        "type": "album",
        "id": "$id"
      }
      },"_source": ""
    }"""


    val queryJson: Json = parse(query).getOrElse(throw new Exception(s"err parse $query"))

    val uri = Properties.elasticSearch.host.withPath(s"""/$index/_search""")

    val s: IO[Request[IO]] = httpPost(uri, queryJson)

    httpClient.fetch(s) {
      x =>
        x.status match {
          case Status.Ok =>
            val a = body(x)
            val ps = a.map { ss =>
              val o = parse(ss).getOrElse(Json.Null)

              val p1 = o.as[ElasticSearchTemplate[ElasticProductBase]]
              val p = p1.map { a =>
                a.hits.hits.map { z =>
                  z._id
                }
              }
              val ps: immutable.Seq[String] = p.getOrElse(throw new Exception(s"err decode $p"))
              ps
            }
            ps
          case e =>
            log.error(s"err $e")
            IO.raiseError(new Exception(e.toString))
        }
    }

  }

  def deleteArtistAndAlbums(index: String, theType: String, artistId: String): IO[String] = {
    val albums = albumsByArtist(index, artistId)

    val o = albums.map { albumList =>
      albumList.map(albumId => deleteAlbum(index, theType, albumId, artistId))
    }

    o.flatMap { _ =>
      val uriArtist = byId(index, theType, artistId)

      val dd = httpDelete(uriArtist)

      httpClient.fetch(dd) { x =>
        x.status match {
          case Status.Ok =>
            body(x)
          case e =>
            log.error(s"err $e")
            IO.raiseError(new Exception(e.toString))
        }
      }
    }

  }


  def deleteAlbum(index: String, theType: String, albumId: String, artistId: String): IO[String] = {
    log.debug("deleteAlbum")
    val uri = routing(index, theType, albumId, artistId)
    log.debug(uri.toString())
    val dd = httpDelete(uri)

    httpClient.fetch(dd) { x =>
      x.status match {
        case Status.Ok =>
          body(x)
        case e =>
          log.error(s"err $e")
          IO.raiseError(new Exception(e.toString))
      }
    }
  }

  def createSchema = {
    val uri = Properties.elasticSearch.host.withPath("/music")
    val b =
      """
        {
            "mappings": {
               "doc":{
                  "properties":{
                     "my_join_field": {
                      "type": "join",
                      "relations": {
                        "artist": "album"
                      }
                    }
                  }
               }
             }
        }
      """.stripMargin

    val json: Json = parse(b).getOrElse(Json.Null)

    val s = httpPut(uri, json)
    httpClient.fetch(s) {
      x =>
        x.status match {
          case Status.Ok =>
            body(x)
          case Status.Created =>
            body(x)
          case e =>
            log.error(s"err $uri $e")
            IO.raiseError(new Exception(body(x) + e.toString()))
        }
    }
  }


}
