package com.gvolpe.social.service

import com.gvolpe.social.model.{Friendship, Person, PersonIdentifier}
import com.gvolpe.social.titan.{SocialNetworkTitanConfiguration, TitanConnection, TitanInMemoryConnection}
import gremlin.scala._
import org.slf4j.LoggerFactory

object DefaultSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

trait SocialNetworkService extends SocialNetworkTitanConfiguration {
  self: TitanConnection =>

  val logger = LoggerFactory.getLogger("SocialNetworkService")

  private def findPerson(personId: Long): Option[Person] = {
    g.V.has(PersonId, personId)
      .value(PersonName)
      .headOption()
      .map(Person.apply(personId, _))
  }

  private def findPersonsBy(personId: Long, link: String): List[Person] = {
    val friends = for {
      f <- g.V.has(PersonId, personId).outE(link).inV()
    } yield Person(f.value2(PersonId), f.value2(PersonName))
    friends.toList()
  }

  def findFollowers(personId: PersonIdentifier): List[Person] = findPersonsBy(personId.id, FollowedBy)

  def findFollowing(personId: PersonIdentifier): List[Person] = findPersonsBy(personId.id, Following)

  def createPerson(person: Person): Option[Person] = findPerson(person.id) match {
    case Some(v)  => None
    case None     =>
      g + (PersonLabel, PersonId -> person.id, PersonName -> person.name)
      g.tx().commit()
      Some(person)
  }

  // TODO: Add validation for existent relationships
  def follow(from: Person, to: Person): Option[Friendship] =
    (findPerson(from.id), findPerson(to.id)) match {
      case (Some(f), Some(t)) =>
        val friendship = for {
          f <- g.V.has(PersonId, from.id)
          t <- g.V.has(PersonId, to.id)
        } yield {
          f --- Following  --> t
          t --- FollowedBy --> f
        }
        friendship.headOption()
        g.tx().commit()
        Some(Friendship(from, to))
      case _ => None
    }

}
