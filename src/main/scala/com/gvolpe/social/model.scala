package com.gvolpe.social

object model {

  case class PersonIdentifier(id: Long) extends AnyVal
  case class Person(id: Long, name: String)
  case class Friendship(from: Person, to: Person)

}
