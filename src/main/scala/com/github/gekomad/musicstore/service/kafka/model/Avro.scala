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

package com.github.gekomad.musicstore.service.kafka.model

object Avro {

  val upsertArtist = "UPSERT_ARTIST"
  val upsertAlbum = "UPSERT_ALBUM"

  final case class AvroPayload(id: String, payload: String)

  type AvroArtist = AvroPayload

  final case class AvroAlbum(idAlbum: String, payload: AvroPayload)

  final case class AvroProduct(theType: String, payload: String)

}

//
//object Avro {
//import org.apache.avro.Schema
//  import com.sksamuel.avro4s.AvroSchema
//  val artistAvro: Schema = AvroSchema[ArtistAvro]
//
//  val schema: Schema = new Schema.Parser().parse(
//    """
//      {"type":"record",
//      "name":"PublishAvro",
//      "namespace":"com.github.gekomad.kafkaScala.test.unit",
//      "fields":[
//          {"name":"id","type":"String"},
//          {"name":"payload","type":"string"}
//      ]}
//    """.stripMargin
//  )
//}
