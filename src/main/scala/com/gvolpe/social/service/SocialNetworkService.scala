package com.gvolpe.social.service

import java.time.Instant

import com.gvolpe.social.model.{Friendship, Person, PersonIdentifier}
import com.gvolpe.social.titan.{SocialNetworkTitanConfiguration, TitanConnection, TitanInMemoryConnection}
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.slf4j.LoggerFactory

object DefaultSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

trait SocialNetworkService extends SocialNetworkTitanConfiguration {
  self: TitanConnection =>

  val logger = LoggerFactory.getLogger("SocialNetworkService")

  private def findPerson(personId: Long): Option[Person] = {
    val person = for {
      p <- g.V.has(PersonId, personId)
    } yield Person(
      personId,
      p.value2(PersonName),
      p.value2(PersonAge),
      p.value2(PersonCountry),
      p.value2(PersonProfession)
    )
    person.headOption()
  }

  def findFollowersFrom(personId: Long, country: String) = {
    val friends = for {
      f <- g.V.has(PersonId, personId).outE(FollowedBy).inV().has(PersonCountry, P.eq(country))
    } yield Person(
      personId,
      f.value2(PersonName),
      f.value2(PersonAge),
      f.value2(PersonCountry),
      f.value2(PersonProfession)
    )
    friends.toList()
  }

  def findFollowersWithAgeRange(personId: Long, from: Int, to: Int) = {
    val friends = for {
      f <- g.V.has(PersonId, personId).outE(FollowedBy).inV().has(PersonAge, P.gte(from)).has(PersonAge, P.lte(to))
    } yield Person(
      personId,
      f.value2(PersonName),
      f.value2(PersonAge),
      f.value2(PersonCountry),
      f.value2(PersonProfession)
    )
    friends.toList()
  }

  private def findPersonsBy(personId: Long, link: String): List[Person] = {
    val friends = for {
      f <- g.V.has(PersonId, personId).outE(link).inV()
    } yield Person(
      personId,
      f.value2(PersonName),
      f.value2(PersonAge),
      f.value2(PersonCountry),
      f.value2(PersonProfession)
    )
    friends.toList()
  }

  def findFollowers(personId: PersonIdentifier): List[Person] = findPersonsBy(personId.id, FollowedBy)

  def findFollowing(personId: PersonIdentifier): List[Person] = findPersonsBy(personId.id, Following)

  def createPerson(person: Person): Option[Person] = findPerson(person.id) match {
    case Some(v)  => None
    case None     =>
      g + (PersonLabel, PersonId          -> person.id,
                        PersonName        -> person.name,
                        PersonAge         -> person.age,
                        PersonCountry     -> person.country,
                        PersonProfession  -> person.profession)
      g.tx().commit()
      Some(person)
  }

  // TODO: Add validation for existent relationships
  def follow(from: Person, to: Person, timestamp: Instant = Instant.now()): Option[Friendship] =
    (findPerson(from.id), findPerson(to.id)) match {
      case (Some(f), Some(t)) =>
        val friendship = for {
          f <- g.V.has(PersonId, from.id)
          t <- g.V.has(PersonId, to.id)
        } yield {
          f --- (Following,  TimestampKey -> timestamp.toEpochMilli) --> t
          t --- (FollowedBy, TimestampKey -> timestamp.toEpochMilli) --> f
        }
        friendship.headOption()
        g.tx().commit()
        Some(Friendship(from, to, timestamp))
      case _ => None
    }

}
