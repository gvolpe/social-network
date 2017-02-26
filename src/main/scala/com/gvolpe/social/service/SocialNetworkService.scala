package com.gvolpe.social.service

import com.gvolpe.social.model.{Friendship, Person}
import com.gvolpe.social.titan.{SocialNetworkTitanConfiguration, TitanConnection, TitanInMemoryConnection}
import gremlin.scala._
import org.slf4j.LoggerFactory

object DefaultSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

trait SocialNetworkService extends SocialNetworkTitanConfiguration {
  self: TitanConnection =>

  val logger = LoggerFactory.getLogger("SocialNetworkService")

  protected def findPerson(personId: Long): Option[Person] = {
    g.V.has(PersonId, personId)
      .value(PersonName)
      .headOption()
      .map(Person.apply(personId, _))
  }

  def findFriends(personId: Long): List[Person] = {
    val friends = for {
      f <- g.V.has(PersonId, personId).outE(FriendLink).inV()
    } yield Person(f.value2(PersonId), f.value2(PersonName))
    friends.toList()
  }

  def createPerson(person: Person): Option[Person] = findPerson(person.id) match {
    case Some(v)  => None
    case None     =>
      g + (PersonLabel, PersonId -> person.id, PersonName -> person.name)
      g.tx().commit()
      Some(person)
  }

  def createFriendship(one: Person, two: Person): Option[Friendship] =
    (findPerson(one.id), findPerson(two.id)) match {
      case (Some(f), Some(t)) =>
        val friendship = for {
          f <- g.V.has(PersonId, one.id)
          t <- g.V.has(PersonId, two.id)
        } yield f <-- FriendLink --> t
        friendship.headOption()
        g.tx().commit()
        Some(Friendship(one, two))
      case _ => None
    }

}
