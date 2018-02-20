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

package com.github.gekomad.musicstore.test.unit

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.github.gekomad.musicstore.utility.MyPredef._
import com.github.gekomad.musicstore.utility.Utility._
import org.scalatest._
import org.slf4j.{Logger, LoggerFactory}

class UtilTest extends FunSuite {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  test("Check Domain") {
    assert(!isDomain("ssss"))
    assert(!isDomain(""))
    assert(!isDomain(null))
    assert(!isDomain("sfsd.m"))
    assert(!isDomain("ssss.r34"))
    assert(!isDomain("fsd.com"))
    assert(!isDomain("jjjjjj.si.it"))
    assert(!isDomain("fAsd.Aom"))

    assert(isDomain("http://sfsd.com"))
    assert(isDomain("http://www.6iyzdxw9e6d.fr"))
    assert(isDomain("https://www.6iyzdxw9e6d.fr"))
    assert(isDomain("www.sfsd.com"))
  }

  test("Trasform localDate to Timestamp") {
    val l = LocalDate.parse("2016-09-12", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val t: Timestamp = localDateToTimestamp(l)
    assert(t.getTime == 1473631200000L)
  }

  test("isUUID") {
    import com.github.gekomad.musicstore.utility.UUIDable._
    import com.github.gekomad.musicstore.utility.UUIDableInstances._
    assert("ce68ef4d-c44f-4936-89a3-adaac691c369".isUUID)
    assert(!"cc".isUUID)
  }
}
