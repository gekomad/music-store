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

import ch.qos.logback.classic
import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec

object Log {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  /*
  *   backup log levels
   */
  def backupLogLevels: Map[String, Level] = {

    @tailrec
    def readLogLevels(bk: Map[String, Level], loggers: List[classic.Logger]): Map[String, Level] = loggers match {
      case Nil => Map.empty
      case h :: Nil => bk + (h.getName -> h.getLevel)
      case h :: t => readLogLevels(bk + (h.getName -> h.getLevel), t)
    }

    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

    val l = loggerContext.getLoggerList.toArray.toList.asInstanceOf[List[classic.Logger]]

    readLogLevels(Map[String, Level](), l)
  }

  /*
    * set levelToSet to all packages but not for excludePackages
    * return old levels
  * */
  def bkAndsetLogLevels(levelToSet: Level, excludePackages: List[String]): Map[String, Level] = {
    val bkLevels = backupLogLevels

    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

    loggerContext.getLoggerList.forEach(x => if (!excludePackages.contains(x.getName)) loggerContext.getLogger(x.getName).setLevel(levelToSet))

    bkLevels
  }

  def setLogLevels(bk: Map[String, Level]): Unit = {
    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    bk.foreach(x => loggerContext.getLogger(x._1).setLevel(bk(x._1)))
  }

}
