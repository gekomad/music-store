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
import com.github.gekomad.musicstore.service.kafka.Producers
import com.github.gekomad.musicstore.service.kafka.model.Avro.{AvroAlbum, AvroArtist, AvroProduct}
import com.github.gekomad.musicstore.utility.Properties
import io.circe.Json
import org.http4s.Status
import org.slf4j.{Logger, LoggerFactory}
import io.circe.generic.auto._
import io.circe.java8.time._
import com.github.gekomad.musicstore.service.kafka.model.Avro
import io.circe.parser.parse
import io.circe.generic.auto._
import cakesolutions.kafka.KafkaProducer
import cats.effect.IO
import com.github.gekomad.musicstore.service.kafka.Producers.KafkaProducer1
import org.apache.kafka.clients.producer.RecordMetadata
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProductService {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  lazy val kafkaProducer = Properties.kafka.map(Producers.KafkaProducer1(_))

  def searchTrack(name: String) = ElasticService.searchTrack(name)()

  def searchArtistByname(name: String) = ElasticService.searchArtistByName(name)()

  def loadArtist(id: String): Future[Option[Tables.ArtistsType]] = ArtistDAO.load(id)

  def loadAlbum(id: String): Future[Option[Tables.AlbumsType]] = AlbumDAO.load(id)

  def artistsCount: Future[Int] = ArtistDAO.count

  def serialiseFutures[A, B](l: Iterable[A])(fn: A => Future[B]): Future[List[B]] =
    l.foldLeft(Future(List.empty[B])) {
      (previousFuture, next) =>
        for {
          previousResults <- previousFuture
          next <- fn(next)
        } yield previousResults :+ next
    }

  def insertAvro(avro: AvroProduct): IO[String] = {

    val payload = avro.payload

    lazy val json: Json = parse(payload).getOrElse(throw new Exception(s"parse error $payload"))
    avro.theType match {

      case Avro.upsertArtist =>

        val avroArtist = json.as[AvroArtist]

        avroArtist match {
          case Left(f) =>
            log.error(s"error decode avroArtist $json", f)
            IO(f.toString)
          case Right(s) =>
            val j = parse(s.payload).getOrElse(throw new Exception(s"parse error $payload"))
            val ob = j.as[ArtistPayload].getOrElse(throw new Exception(s"parse error $payload"))

            upsertArtist(s.id, ob).attempt.flatMap {
              _.toTry match {
                case Failure(f) => log.error(s"error to store in db id $json Insert in kafka dlq", f)
                  Properties.kafka.map(kafka => Producers.KafkaProducerDlq(kafka).upsertArtist(s.id, payload))
                  IO.raiseError(new Exception(f.toString))
                case Success(ss) => log.debug(s"ok stored in db ${s.id}")
                  IO(ss)
              }
            }
        }

      case Avro.upsertAlbum =>

        val avroAlbum = json.as[AvroAlbum]

        avroAlbum match {
          case Left(f) =>
            log.error(s"error decode avroArtist $json", f)
            IO(f.toString)
          case Right(s) =>
            val j = parse(s.payload.payload).getOrElse(throw new Exception(s"parse error $payload"))
            val ob = j.as[AlbumPayload].getOrElse(throw new Exception(s"parse error $payload"))

            upsertAlbum(s.payload.id, s.idAlbum, ob).attempt.flatMap {
              _.toTry match {
                case Failure(f) => log.error(s"error to store in db idArtist: ${s.payload.id} idAlbum: ${s.idAlbum}. Insert in kafka dlq", f)
                  Properties.kafka.map(kafka => Producers.KafkaProducerDlq(kafka).upsertArtist(s.idAlbum, payload))
                  IO.raiseError(new Exception(f.toString))
                case Success(ss) => log.debug(s"ok stored in db idArtist: ${s.payload.id} idAlbum: ${s.idAlbum}")
                  IO(ss)
              }
            }
        }
    }

  }

  def insertAvroFuture(avro: AvroProduct): Future[IO[String]] = Future(insertAvro(avro))

  def storeList(avroList: List[AvroProduct]): Future[Iterator[List[IO[String]]]] = {
    log.debug(s"storeList size: ${avroList.size}")

    val s = avroList.sliding(Properties.sql.maxParallelUpsert).map(list => serialiseFutures(list)(insertAvroFuture(_)))

    Future.sequence(s)

  }


  def upsertArtist(id: String, artist: ArtistPayload, json: String): IO[String] = {
    log.debug(s"update artist $id $json")
    kafkaProducer.map { f =>
      val o = f.upsertArtist(id, json).map(_.mkString("|"))
      IO.fromFuture(IO(o))
    }.getOrElse {
      upsertArtist(id, artist)
    }
  }

  def upsertArtist(id: String, payload: ArtistPayload): IO[String] = {
    log.debug(s"upsertArtist artist $id")
    val p1: Future[Int] = SqlService.upsertArtist(id, payload)
    val o = p1.map { _ =>
      ElasticService.insert[ElasticArtist](id, Properties.elasticSearch.index1, ElasticArtist(payload))
    }
    IO.fromFuture(IO(o)).flatMap(a => a)
  }

  def upsertAlbum(idArtist: String, id: String, payload: AlbumPayload): IO[String] = {
    log.debug(s"upsertArtist album $id")
    val o = SqlService.upsertAlbum(id, idArtist, payload).map { _ =>
      ElasticService.insert[ElasticAlbum](id, Properties.elasticSearch.index1, ElasticAlbum(idArtist, payload))
    }
    IO.fromFuture(IO(o)).flatMap(a => a)

  }

  def deleteArtist(id: String): IO[String] = {
    lazy val deleteArtistAndAlbumFromNOSQL = ElasticService.deleteArtistAndAlbums(Properties.elasticSearch.index1, Properties.elasticSearch.artistType, id)
    val deleteArtistAndAlbumFromSQL = ArtistDAO.delete(id).map(_ => Status.Ok)
    val o = deleteArtistAndAlbumFromSQL.map(_ => deleteArtistAndAlbumFromNOSQL)
    IO.fromFuture(IO(o)).flatMap(a => a)
  }

  def deleteAlbum(id: String, artistId: String): IO[String] = { //TODO add test
    val o = AlbumDAO.delete(id).map { _ =>
      ElasticService.deleteAlbum(Properties.elasticSearch.index1, Properties.elasticSearch.albumType, id, artistId)
    }
    IO.fromFuture(IO(o)).flatMap(a => a)
  }

  def upsertAlbum(idArtist: String, idAlbum: String, json: String): IO[String] = {
    log.debug(s"insert album $json")
    val albumTry = AlbumPayload(json)
    albumTry match {
      case Failure(f) =>
        throw new Exception(f)
      case Success(album) => kafkaProducer.fold {
        upsertAlbum(idArtist, idAlbum, album)
      } { a =>
        val o = a.upsertAlbum(idArtist, idAlbum, json).map(_.mkString("|")).map(IO(_))
        IO.fromFuture(IO(o)).flatMap(a => a)
      }
    }
  }

  def readDql = { // TODO
    log.debug("readDql")
    ???
  }


}
