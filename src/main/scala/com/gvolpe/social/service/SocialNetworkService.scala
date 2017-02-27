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

  private val mapPerson: Vertex => Person = p => Person(
    p.value2(PersonId),
    p.value2(PersonName),
    p.value2(PersonAge),
    p.value2(PersonCountry),
    p.value2(PersonProfession)
  )

  private def findPerson(personId: Long): Option[Person] = {
    g.V.has(PersonId, personId).map(mapPerson).headOption()
  }

  // TODO: Maybe also show the different paths if there's more than one?
  def findPresidentByCommonConnections(personId: Long, country: String): Option[Person] = {
    g.V.has(PersonId, personId)
      .repeat(_.outE(Following).inV.has(PersonCountry, P.eq(country)).simplePath)
      .until(_.has(PersonProfession, "President"))
      .map(mapPerson)
      .headOption()
  }

  def followersFromCount(personId: Long, country: String): Long = {
    g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .inV()
      .has(PersonCountry, P.eq(country))
      .count()
      .head()
  }

  def findFollowersFrom(personId: Long, country: String) = {
    val persons = g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .inV()
      .has(PersonCountry, P.eq(country))
      .map(mapPerson)
    persons.toList()
  }

  def findFollowersSince(personId: Long, since: Instant) = {
    val persons = g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .has(TimestampKey, P.gte(since.toEpochMilli))
      .inV()
      .map(mapPerson)
    persons.toList()
  }

  def findFirstFollowerOfTopOne(personId: Long) = {
    val result = for {
      f <- g.V.has(PersonId, personId).outE(Following).orderBy(TimestampKey.value).inV()
      g <- g.V(f).hasLabel(PersonLabel).outE(FollowedBy).orderBy(TimestampKey.value).inV().map(mapPerson)
    } yield (mapPerson(f), g)
    result.headOption()
  }

  def findFollowersWithAgeRange(personId: Long, from: Int, to: Int) = {
    val persons = g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .inV()
      .has(PersonAge, P.gte(from)).has(PersonAge, P.lte(to))
      .map(mapPerson)
    persons.toList()
  }

  private def findPersonsBy(personId: Long, link: String): List[Person] = {
    val persons = g.V.has(PersonId, personId)
      .outE(link)
      .inV()
      .map(mapPerson)
    persons.toList()
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
