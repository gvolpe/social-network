package com.gvolpe.social

import java.time.Instant

object model {

  case class PersonIdentifier(id: Long) extends AnyVal
  case class Country(value: String) extends AnyVal

  case class Person(id: Long, name: String, age: Int, country: String, profession: String)
  case class Friendship(from: Person, to: Person, timestamp: Instant)

  sealed trait FriendshipException extends Product with Serializable
  case class PersonNotFound(person: Person) extends FriendshipException
  case class FriendshipAlreadyExists(from: Person, to: Person) extends FriendshipException

}
