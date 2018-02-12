package com.github.gekomad.musicstore.utility


trait UUIDable[A] {

  def isUUID(id: A): Boolean
}


object UUIDable {

  implicit class a[A](value: A) {

    def isUUID(implicit p: UUIDable[A]): Boolean = p.isUUID(value)
  }

}

import java.util.UUID

import scala.util.{Success, Try}

object UUIDableInstances {


  implicit val b = new UUIDable[String] {
    def isUUID(value: String) = Try(UUID.fromString(value)) match {
      case Success(uuid) => uuid.toString.toUpperCase == value.toUpperCase
      case _ => false
    }
  }

}
