package com.gvolpe.social.service

import java.time.Instant

import com.gvolpe.social.model._
import com.gvolpe.social.titan.{TitanConnection, TitanInMemoryConnection}
import com.gvolpe.social.titan.SocialNetworkTitanConfiguration._
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.slf4j.LoggerFactory

import scalaz.\/
import scalaz.concurrent.Task
import scalaz.Scalaz._

object DefaultSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

trait SocialNetworkService {
  self: TitanConnection =>

  val logger = LoggerFactory.getLogger("SocialNetworkService")

  private def findPerson(personId: Long): Option[Person] = {
    g.V.has(PersonId, personId).map(_.mapPerson).headOption()
  }

  private def findPresident(personId: Long, country: String) = {
    g.V.has(PersonId, personId)
      .repeat(_.outE(Following).inV.has(PersonCountry, P.eq(country)).simplePath)
      .until(_.has(PersonProfession, "President"))
  }

  def findPresidentByCommonConnections(personId: PersonIdentifier, country: Country): Option[Person] = {
    findPresident(personId.id, country.value).map(_.mapPerson).headOption()
  }

  def findPathToPresident(personId: PersonIdentifier, country: Country): List[Person] = {
    val path = findPresident(personId.id, country.value).path().headOption().toList
    path.flatMap { p  => (0 until p.size()).map(personFromPath(p, _)) }.flatten
  }

  private def followersFrom(personId: Long, country: String) = {
    g.V.has(PersonId, personId)
      .outE(FollowedBy)
      .inV()
      .has(PersonCountry, P.eq(country))
  }

  def followersFromCount(personId: PersonIdentifier, country: Country): Long = {
    followersFrom(personId.id, country.value).count().head()
  }

  def findFollowersFrom(personId: PersonIdentifier, country: Country): List[Person] = {
    followersFrom(personId.id, country.value).map(_.mapPerson).toList()
  }

  def findFollowersSince(personId: PersonIdentifier, since: Instant): List[Person] = {
    g.V.has(PersonId, personId.id)
      .outE(FollowedBy)
      .has(TimestampKey, P.gte(since.toEpochMilli))
      .inV()
      .map(_.mapPerson)
      .toList()
  }

  def findFirstFollowerOfTopOne(personId: PersonIdentifier): Option[(Person, Person)] = {
    val result = for {
      f <- g.V.has(PersonId, personId.id).outE(Following).orderBy(TimestampKey.value).inV()
      g <- g.V(f).hasLabel(PersonLabel).outE(FollowedBy).orderBy(TimestampKey.value).inV()
    } yield (f.mapPerson, g.mapPerson)
    result.headOption()
  }

  def findFollowersWithAgeRange(personId: PersonIdentifier, from: Int, to: Int): List[Person] = {
    g.V.has(PersonId, personId.id)
      .outE(FollowedBy)
      .inV()
      .has(PersonAge, P.gte(from)).has(PersonAge, P.lte(to))
      .map(_.mapPerson)
      .toList()
  }

  private def findPersonsBy(personId: Long, link: String): List[Person] = {
    g.V.has(PersonId, personId)
      .outE(link)
      .inV()
      .map(_.mapPerson)
      .toList()
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

  private def findRelationship(from: Person, to: Person): Option[Vertex] = {
    g.V.has(PersonId, from.id)
      .outE()
      .hasLabel(Following)
      .inV()
      .has(PersonId, to.id)
      .headOption()
  }

  def follow(from: Person, to: Person, timestamp: Instant = Instant.now()): Task[FriendshipException \/ Friendship] =
    (findPerson(from.id), findPerson(to.id), findRelationship(from, to)) match {
      case (Some(f), Some(t), None) =>
        val friendship = for {
          f <- g.V.has(PersonId, from.id)
          t <- g.V.has(PersonId, to.id)
        } yield {
          f --- (Following,  TimestampKey -> timestamp.toEpochMilli) --> t
          t --- (FollowedBy, TimestampKey -> timestamp.toEpochMilli) --> f
        }
        friendship.headOption()
        g.tx().commit()
        Task.delay { Friendship(from, to, timestamp).right }
      case (None, _, _)     => Task.delay { PersonNotFound(from).left }
      case (_, None, _)     => Task.delay { PersonNotFound(to).left }
      case (_, _, Some(f))  => Task.delay { FriendshipAlreadyExists(from, to).left }
    }

}
