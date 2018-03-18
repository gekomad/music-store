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

package com.github.gekomad.musicstore.utility

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date
import com.sksamuel.avro4s._
import io.circe.Decoder.Result
import io.circe._

import scala.language.implicitConversions

object MyPredef {

  def isBlank(s: String): Boolean = if (s == null || s.trim.isEmpty) true else false

  implicit def localDateToTimestamp(s: LocalDate): Timestamp = new Timestamp(java.sql.Date.valueOf(s).getTime)

  def now: Timestamp = new Timestamp(new Date().getTime)

  implicit val timestampFormat: Encoder[Timestamp] with Decoder[Timestamp] = new Encoder[Timestamp] with Decoder[Timestamp] {
    implicit override def apply(a: Timestamp): Json = Encoder.encodeLong.apply(a.getTime)

    implicit override def apply(c: HCursor): Result[Timestamp] = Decoder.decodeLong.map(s => new Timestamp(s)).apply(c)

  }

  def serializeAvro[A: SchemaFor : ToRecord](a: A): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val output = AvroOutputStream.binary[A](baos)
    output.write(a)
    output.close()
    baos.toByteArray
  }

  def deserializeAvro[A >: Null : SchemaFor : FromRecord](message: Array[Byte]): Seq[A] = {
    val in = new ByteArrayInputStream(message)
    val input = AvroInputStream.binary[A](in)
    input.iterator.toSeq
  }

}
