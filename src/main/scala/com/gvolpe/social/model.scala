package com.gvolpe.social

object model {

  case class Person(id: Long, name: String)
  case class Friendship(one: Person, two: Person)

}
