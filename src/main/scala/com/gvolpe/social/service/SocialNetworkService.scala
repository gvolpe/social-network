package com.gvolpe.social.service

import java.time.Instant

import com.gvolpe.social.model.{Friendship, Person, PersonIdentifier}
import com.gvolpe.social.titan.{TitanConnection, TitanInMemoryConnection}
import com.gvolpe.social.titan.SocialNetworkTitanConfiguration._
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.slf4j.LoggerFactory

object DefaultSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

trait SocialNetworkService {
  self: TitanConnection =>

  val logger = LoggerFactory.getLogger("SocialNetworkService")

  private def findPerson(personId: Long): Option[Person] = {
    g.V.has(PersonId, personId).map(_.mapPerson).headOption()
  }

  def findPresidentByCommonConnections(personId: Long, country: String): Option[Person] = {
    g.V.has(PersonId, personId)
      .repeat(_.outE(Following).inV.has(PersonCountry, P.eq(country)).simplePath)
      .until(_.has(PersonProfession, "President"))
      .map(_.mapPerson)
      .headOption()
  }

  def findPathToPresident(personId: Long, country: String): List[Person] = {
    val path = g.V.has(PersonId, personId)
      .repeat(_.outE(Following).inV.has(PersonCountry, P.eq(country)).simplePath)
      .until(_.has(PersonProfession, "President"))
      .path()
      .toList()
    path.flatMap { p  =>
      (0 until p.size()).map(i => personFromPath(p, i))
    }.flatten
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
      .map(_.mapPerson)
    persons.toList()
  }

  def findFollowersSince(personId: Long, since: Instant) = {
    val persons = g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .has(TimestampKey, P.gte(since.toEpochMilli))
      .inV()
      .map(_.mapPerson)
    persons.toList()
  }

  def findFirstFollowerOfTopOne(personId: Long) = {
    val result = for {
      f <- g.V.has(PersonId, personId).outE(Following).orderBy(TimestampKey.value).inV()
      g <- g.V(f).hasLabel(PersonLabel).outE(FollowedBy).orderBy(TimestampKey.value).inV()
    } yield (f.mapPerson, g.mapPerson)
    result.headOption()
  }

  def findFollowersWithAgeRange(personId: Long, from: Int, to: Int) = {
    val persons = g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .inV()
      .has(PersonAge, P.gte(from)).has(PersonAge, P.lte(to))
      .map(_.mapPerson)
    persons.toList()
  }

  private def findPersonsBy(personId: Long, link: String): List[Person] = {
    val persons = g.V.has(PersonId, personId)
      .outE(link)
      .inV()
      .map(_.mapPerson)
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
