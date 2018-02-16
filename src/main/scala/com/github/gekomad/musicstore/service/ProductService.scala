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


import com.github.gekomad.musicstore.model.json.elasticsearch.Products.{ElasticAlbum, ElasticArtist}
import com.github.gekomad.musicstore.model.json.in.AlbumPayload
import com.github.gekomad.musicstore.model.json.in.ProductBase.ArtistPayload
import com.github.gekomad.musicstore.model.sql.Tables
import com.github.gekomad.musicstore.service
import com.github.gekomad.musicstore.service.AlbumDAO.artistIdByAlbumId
import com.github.gekomad.musicstore.service.kafka.Producers
import com.github.gekomad.musicstore.service.kafka.model.Avro.{AvroAlbum, AvroArtist, AvroPayload, AvroProduct}
import com.github.gekomad.musicstore.utility.Properties
import fs2.Task
import io.circe.Json
import org.http4s.Response
import org.http4s.dsl.Ok
import org.slf4j.{Logger, LoggerFactory}
import io.circe.parser.parse
import io.circe.syntax._
import java.time.LocalDate

import io.circe.Decoder.Result
import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.syntax._
import java.time.LocalDate

import com.github.gekomad.musicstore.service.kafka.model.Avro
import io.circe.Decoder.Result
import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.java8.time._
import java.io

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ProductService {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  lazy val kafkaProducer = Properties.kafka.map(kafka => Producers.KafkaProducer1(kafka))

  implicit val strategy = fs2.Strategy.fromFixedDaemonPool(10)

  def searchTrack(name: String) = ElasticService.searchTrack(name)()

  def searchArtistByname(name: String) = ElasticService.searchArtistByName(name)()

  def loadArtist(id: String): Future[Option[Tables.ArtistsType]] = ArtistDAO.load(id)

  def loadAlbum(id: String): Future[Option[Tables.AlbumsType]] = AlbumDAO.load(id)

  def storeList(avroList: List[AvroProduct]) = {
    log.debug("storeList")

    val a = avroList.map { avro =>
      val payload = avro.payload

      val json: Json = parse(payload).getOrElse(throw new Exception(s"parse error $payload"))
      avro.theType match {

        case Avro.upsertArtist =>

          val avroArtist = json.as[AvroArtist]

          avroArtist match {
            case Left(f) =>
              log.error(s"error decode avroArtist $json", f)
              throw f
            case Right(s) =>
              val j = parse(s.payload).getOrElse(throw new Exception(s"parse error $payload"))
              val ob = j.as[ArtistPayload].getOrElse(throw new Exception(s"parse error $payload"))

              val o = upsertArtist(s.id, ob)
              o.map {
                ss =>
                  log.debug(s"ok created in db ${s.id}")
                  ss
              }.recover {
                case f =>
                  log.error(s"error to create in db id $json Insert in kafka dlq", f)
                  Properties.kafka.map(kafka => Producers.KafkaProducerDlq(kafka).upsertArtist(s.id, payload))
                  f
              }
          }

        case Avro.upsertAlbum =>

          val avroAlbum = json.as[AvroAlbum]

          avroAlbum match {
            case Left(f) =>
              log.error(s"error decode avroArtist $json", f)
              throw f
            case Right(s) =>
              val j = parse(s.payload.payload).getOrElse(throw new Exception(s"parse error $payload"))
              val ob = j.as[AlbumPayload].getOrElse(throw new Exception(s"parse error $payload"))

              val o = upsertAlbum(s.payload.id, s.idAlbum, ob)
              o.map {
                ss =>
                  log.debug(s"ok created in db idArtist: ${s.payload.id} idAlbum: ${s.idAlbum}")
                  ss
              }.recover {
                case f =>
                  log.error(s"error to create in db idArtist: ${s.payload.id} idAlbum: ${s.idAlbum}. Insert in kafka dlq", f)
                  Properties.kafka.map(kafka => Producers.KafkaProducerDlq(kafka).upsertArtist(s.idAlbum, payload))
                  f
              }
          }

      }
    }
    val p = Future.sequence(a)
    p
  }

  def upsertArtist(id: String, payload: ArtistPayload): Future[io.Serializable] = {

    log.debug(s"upsertArtist artist $id")

    val p1: Future[Int] = SqlService.upsertArtist(id, payload)

    p1.flatMap { _ =>
      ElasticService.insert[ElasticArtist](id, Properties.elasticSearch.index1, ElasticArtist(payload)).unsafeRunAsyncFuture()
    }

  }

  def upsertAlbum(idArtist: String, id: String, payload: AlbumPayload): Future[io.Serializable] = {

    log.debug(s"upsertArtist album $id")

    val p1: Future[Int] = SqlService.upsertAlbum(id, idArtist, payload)

    p1.flatMap { _ =>
      ElasticService.insert[ElasticAlbum](id, Properties.elasticSearch.index1, ElasticAlbum(idArtist, payload)).unsafeRunAsyncFuture()
    }

  }

  def deleteArtist(id: String) = {
    val deleteArtistAndAlbumFromNOSQL = ElasticService.deleteArtistAndAlbums(Properties.elasticSearch.index1, Properties.elasticSearch.artistType, id)

    val deleteArtistAndAlbumFromSQL = ArtistDAO.delete(id).map(a => Response(Ok))

    Task.fromFuture(deleteArtistAndAlbumFromSQL).flatMap(_ => deleteArtistAndAlbumFromNOSQL)
  }

  def deleteAlbum(id: String, artistId: String): Task[Response] = {
    val o = AlbumDAO.delete(id)
    val p = o.map { _ =>
      ElasticService.deleteAlbum(Properties.elasticSearch.index1, Properties.elasticSearch.albumType, id, artistId)
    }
    Task.fromFuture(p).flatMap(a => a)
  }

  def upsertArtist(id: String, json: String): Future[java.io.Serializable] = {

    log.debug(s"update artist $json")
    val artistTry = ArtistPayload(json)

    artistTry match {
      case Failure(f) => throw new Exception(f)
      case Success(artist) => kafkaProducer.map(_.upsertArtist(id, json)).getOrElse(upsertArtist(id, artist))

    }

  }

  def upsertAlbum(idArtist: String, idAlbum: String, json: String): Future[java.io.Serializable] = {

    log.debug(s"insert album $json")
    val albumTry = AlbumPayload(json)

    albumTry match {
      case Failure(f) =>
        throw new Exception(f)
      case Success(album) => kafkaProducer.map(_.upsertAlbum(idArtist, idAlbum, json)).getOrElse(upsertAlbum(idArtist, idAlbum, album))
    }
  }

  def readDql = { // TODO
    log.debug("readDql")
    ???
  }


}
