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

import com.github.gekomad.musicstore.utility.MyPredef._
import org.scalacheck.Gen.{const, frequency}
import org.scalacheck.{Gen, Prop, Properties}

object ScalaCheckTest extends Properties("UtilTest2") {

  val alphaStrAndNull: Gen[String] = frequency(
    (20, Gen.alphaStr),
    (1, null: String)
  )

  property("test isBlank") =
    Prop.forAll(alphaStrAndNull) {
      case s@null => isBlank(s)
      case a if a.trim.isEmpty => isBlank(a)
      case b => !isBlank(b)
    }
}
