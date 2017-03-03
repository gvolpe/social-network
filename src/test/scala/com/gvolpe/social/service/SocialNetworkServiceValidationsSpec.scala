package com.gvolpe.social.service

import java.time.Instant

import com.gvolpe.social.model._
import com.gvolpe.social.titan.TitanInMemoryConnection
import org.scalatest.{FlatSpecLike, Matchers}

import scalaz.{-\/, \/-}

class SocialNetworkServiceValidationsSpec extends SocialNetworkServiceValidationsFixture {

  it should "not create an existent person" in {
    val service = createService()

    val p1 = Person(1, "Gabi", 29, "Argentina", "Software Engineer")

    service.createPerson(p1) should be (Some(p1))
    service.createPerson(p1) should be (None)
  }

  it should "not create an existent follower" in {
    val service = createService()

    val p1 = Person(1, "Gabi", 29, "Argentina", "Software Engineer")
    val p2 = Person(2, "Damian", 29, "Argentina", "Software Engineer")
    val timestamp = Instant.parse("2015-07-01T00:00:00.00Z")

    service.createPerson(p1)
    service.createPerson(p2)

    service.createFollower(p1, p2, timestamp).unsafePerformSync should be (\/-(Friendship(p1, p2, timestamp)))
    service.createFollower(p1, p2, timestamp).unsafePerformSync should be (-\/(FriendshipAlreadyExists(p1, p2)))
  }

  it should "not create a follower if one of the persons does not exist" in {
    val service = createService()

    val p1 = Person(1, "Gabi", 29, "Argentina", "Software Engineer")
    val p2 = Person(2, "Damian", 29, "Argentina", "Software Engineer")
    val timestamp = Instant.parse("2015-07-01T00:00:00.00Z")

    service.createPerson(p1)

    service.createFollower(p1, p2, timestamp).unsafePerformSync should be (-\/(PersonNotFound(p2)))
    service.createFollower(p2, p1, timestamp).unsafePerformSync should be (-\/(PersonNotFound(p2)))
  }

}

trait SocialNetworkServiceValidationsFixture extends FlatSpecLike with Matchers {

  def createService() = new SocialNetworkService with TitanInMemoryConnection

}
