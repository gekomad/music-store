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

//////////////////////////////////////////

trait Defaultable[A] {
  def getOrDefault(value: => A, d: A): A
}

object DefaultableInstances {

  implicit val stringDefaultable = new Defaultable[String] {
    def getOrDefault(input: => String, default: String) = Try(input).getOrElse(default)
  }

  implicit val intDefaultable = new Defaultable[Int] {
    def getOrDefault(input: => Int, default: Int) = Try(input).getOrElse(default)
  }

  implicit val boolDefaultable = new Defaultable[Boolean] {
    def getOrDefault(input: => Boolean, default: Boolean) = Try(input).getOrElse(default)
  }

}

object Defaultable {
  def getOrDefault[A](input: => A, default: A)(implicit p: Defaultable[A]): A =
    p.getOrDefault(input, default)
}

