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

import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID



import scala.collection.immutable
import scala.util.Random

object MyRandom {

  val extensions = List(".com", ".it", ".eu", ".info", ".fr", ".co.uk")

  def getRandomUUID: UUID = UUID.randomUUID

  def getRandomString(lung: Int): String = scala.util.Random.alphanumeric.take(lung).mkString

  def getRandomInt(from: Int, until: Int): Int = Random.shuffle(from to until).take(1).head

  def getRandomStringList(a: Int, b: Int): List[String] = (1 to getRandomInt(a)+1).map(_ => getRandomString(b)).toList

  def getRandomInt(until: Int): Int = getRandomInt(0, until)

  def getRandomLocalDate: LocalDate = LocalDate.of(getRandomInt(1973, 2018), getRandomInt(1, 12), getRandomInt(1, 28))

  def getRandomLong: Long = scala.util.Random.nextLong()

  def getRandomFloat: Float = scala.util.Random.nextFloat()

  def getRandomBoolean: Boolean = Random.nextBoolean

  def getRandomUrl: String = "http://www." + (getRandomString(getRandomInt(10) + 3) + extensions(getRandomInt(extensions.length - 1))).toLowerCase

  def getRandomBigString(l: Int = 100): String = getRandomString(l).replace('k', ' ').replace('z', ' ').replace('j', ' ')

  def getRandomTimestamp: Timestamp = {
    val unixtime = 1293861599 + scala.util.Random.nextDouble() * 60 * 60 * 24 * 365
    val o = new java.util.Date(unixtime.toLong).getTime
    new Timestamp(o)
  }

}
