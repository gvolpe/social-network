package com.gvolpe.social.service

import java.time.Instant

import com.gvolpe.social.model._
import com.gvolpe.social.titan.TitanInMemoryConnection
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}

class SocialNetworkServiceSpec extends SocialNetworkServiceFixture {

  override def beforeAll() = {
    createPersonsAndConnections()
  }

  override def afterAll() = {
    println("Closing Titan In Memory connection...")
    testService.g.close()
  }

  forAll(followingExample) { (description, personId, expected) =>
    it should description in {
      testService.findFollowing(personId).sortBy(_.id) should be (expected)
    }
  }

  forAll(followedByExample) { (description, personId, expected) =>
    it should description in {
      testService.findFollowers(personId).sortBy(_.id) should be (expected)
    }
  }

  forAll(followersByAgeExample) { (description, personId, from, to, expected) =>
    it should s"$description $from and $to" in {
      testService.findFollowersWithAgeRange(personId, from, to).sortBy(_.id) should be (expected)
    }
  }

  forAll(followersFromCountryExample) { (description, personId, country, expected) =>
    it should description in {
      testService.findFollowersFrom(personId, country).sortBy(_.id) should be (expected)
      testService.followersFromCount(personId, country)             should be (expected.size)
    }
  }

  forAll(presidentReachabilityExample) { (description, personId, country, expected, path) =>
    it should description in {
      testService.findPresidentByCommonConnections(personId, country) should be (expected)
      testService.findPathToPresident(personId, country)              should be (path)
    }
  }

  forAll(followersSinceDateExample) { (description, personId, date, expected) =>
    it should description in {
      testService.findFollowersSince(personId, date).sortBy(_.id) should be (expected)
    }
  }

  forAll(firstFollowersOfExample) { (description, personId, expected) =>
    it should description in {
      testService.findFirstFollowerOfTopOne(personId) should be (expected)
    }
  }

}

trait SocialNetworkServiceFixture extends FlatSpecLike
                                  with Matchers
                                  with BeforeAndAfterEach
                                  with BeforeAndAfterAll
                                  with PropertyChecks {

  def createService() = new SocialNetworkService with TitanInMemoryConnection

  val testService = createService()

  val p1 = Person(1, "Gabi", 29, "Argentina", "Software Engineer")
  val p2 = Person(2, "Damian", 29, "Argentina", "Software Engineer")
  val p3 = Person(3, "John", 30, "Ireland", "Pilot")
  val p4 = Person(4, "Mike", 32, "England", "Architect")
  val p5 = Person(5, "Chris", 22, "Germany", "Marketing")
  val p6 = Person(6, "Guille", 24, "Argentina", "Sales")
  val p7 = Person(7, "Ivan", 21, "Argentina", "Student")
  val p8 = Person(8, "Gustavo", 35, "Argentina", "Lawyer")
  val p9 = Person(9, "Capusotto", 57, "Argentina", "President")

  def follow(from: Person, to: Person, timestamp: Instant) = {
    testService.createFollower(from, to, timestamp).unsafePerformSync
  }

  def createPersonsAndConnections() = {
    println("Creating persons and followers...")
    testService.createPerson(p1)
    testService.createPerson(p2)
    testService.createPerson(p3)
    testService.createPerson(p4)
    testService.createPerson(p5)
    testService.createPerson(p6)
    testService.createPerson(p7)
    testService.createPerson(p8)
    testService.createPerson(p9)

    follow(p1, p2, Instant.parse("2015-07-01T00:00:00.00Z")) // Gabi --> Damian
    follow(p1, p3, Instant.parse("2015-11-25T00:00:00.00Z")) // Gabi --> John
    follow(p1, p6, Instant.parse("2007-01-23T00:00:00.00Z")) // Gabi --> Guille
    follow(p2, p1, Instant.parse("2015-08-14T00:00:00.00Z")) // Damian --> Gabi
    follow(p2, p3, Instant.parse("2014-10-20T00:00:00.00Z")) // Damian --> John
    follow(p2, p8, Instant.parse("2009-05-18T00:00:00.00Z")) // Damian --> Gustavo
    follow(p3, p5, Instant.parse("2010-05-17T00:00:00.00Z")) // John --> Chris
    follow(p4, p1, Instant.parse("2012-03-07T00:00:00.00Z")) // Mike --> Gabi
    follow(p4, p6, Instant.parse("2010-07-01T00:00:00.00Z")) // Mike --> Guille
    follow(p6, p1, Instant.parse("2017-02-27T00:00:00.00Z")) // Guille --> Gabi
    follow(p7, p1, Instant.parse("2017-03-01T00:00:00.00Z")) // Ivan --> Gabi
    follow(p8, p9, Instant.parse("2008-06-13T00:00:00.00Z")) // Gustavo --> Capusotto
    follow(p9, p8, Instant.parse("2016-08-02T00:00:00.00Z")) // Capusotto --> Gustavo
  }

  val followingExample = Table(
    ("description", "personId", "expected"),
    (s"${p1.name} is following", PersonIdentifier(p1.id), List(p2, p3, p6)),
    (s"${p2.name} is following", PersonIdentifier(p2.id), List(p1, p3, p8)),
    (s"${p3.name} is following", PersonIdentifier(p3.id), List(p5)),
    (s"${p4.name} is following", PersonIdentifier(p4.id), List(p1, p6)),
    (s"${p6.name} is following", PersonIdentifier(p6.id), List(p1)),
    (s"${p7.name} is following", PersonIdentifier(p7.id), List(p1)),
    (s"${p8.name} is following", PersonIdentifier(p8.id), List(p9)),
    (s"${p9.name} is following", PersonIdentifier(p9.id), List(p8))
  )

  val followedByExample = Table(
    ("description", "personId", "expected"),
    (s"${p1.name} is followed by", PersonIdentifier(p1.id), List(p2, p4, p6, p7)),
    (s"${p2.name} is followed by", PersonIdentifier(p2.id), List(p1)),
    (s"${p3.name} is followed by", PersonIdentifier(p3.id), List(p1, p2)),
    (s"${p5.name} is followed by", PersonIdentifier(p5.id), List(p3)),
    (s"${p6.name} is followed by", PersonIdentifier(p6.id), List(p1, p4)),
    (s"${p8.name} is followed by", PersonIdentifier(p8.id), List(p2, p9)),
    (s"${p9.name} is followed by", PersonIdentifier(p9.id), List(p8))
  )

  val followersByAgeExample = Table(
    ("description", "personId", "from", "to", "expected"),
    (s"${p1.name}'s followers with ages between", PersonIdentifier(p1.id), 20, 25, List(p6, p7)),
    (s"${p2.name}'s followers with ages between", PersonIdentifier(p2.id), 20, 25, List.empty[Person]),
    (s"${p3.name}'s followers with ages between", PersonIdentifier(p3.id), 20, 25, List.empty[Person]),
    (s"${p4.name}'s followers with ages between", PersonIdentifier(p4.id), 20, 25, List.empty[Person]),
    (s"${p5.name}'s followers with ages between", PersonIdentifier(p5.id), 20, 25, List.empty[Person]),
    (s"${p6.name}'s followers with ages between", PersonIdentifier(p6.id), 20, 25, List.empty[Person]),
    (s"${p7.name}'s followers with ages between", PersonIdentifier(p7.id), 20, 25, List.empty[Person]),
    (s"${p8.name}'s followers with ages between", PersonIdentifier(p8.id), 20, 25, List.empty[Person]),
    (s"${p9.name}'s followers with ages between", PersonIdentifier(p9.id), 20, 25, List.empty[Person]),
    (s"${p1.name}'s followers with ages between", PersonIdentifier(p1.id), 30, 40, List(p4)),
    (s"${p9.name}'s followers with ages between", PersonIdentifier(p9.id), 30, 40, List(p8))
  )

  val followersFromCountryExample = Table(
    ("description", "personId", "country", "expected"),
    (s"${p1.name}'s followers from Argentina", PersonIdentifier(p1.id), Country(p1.country), List(p2, p6, p7)),
    (s"${p1.name}'s followers from Vietnam", PersonIdentifier(p1.id), Country("Vietnam"), List.empty[Person]),
    (s"${p2.name}'s followers from ${p2.country}", PersonIdentifier(p2.id), Country(p2.country), List(p1)),
    (s"${p3.name}'s followers from ${p3.country}", PersonIdentifier(p3.id), Country(p3.country), List.empty[Person]),
    (s"${p3.name}'s followers from Argentina", PersonIdentifier(p3.id), Country("Argentina"), List(p1, p2))
  )

  val presidentReachabilityExample = Table(
    ("description", "personId", "country", "expected", "path"),
    (s"Can ${p1.name} reach the president of ${p1.country} by common connections?", PersonIdentifier(p1.id), Country(p1.country), Some(p9), List(p1, p2, p8, p9)),
    (s"Can ${p2.name} reach the president of ${p2.country} by common connections?", PersonIdentifier(p2.id), Country(p2.country), Some(p9), List(p2, p8, p9)),
    (s"Can ${p8.name} reach the president of ${p8.country} by common connections?", PersonIdentifier(p8.id), Country(p8.country), Some(p9), List(p8, p9)),
    (s"Can ${p9.name} reach the president of ${p9.country} by common connections?", PersonIdentifier(p9.id), Country(p9.country), None, List.empty[Person]),
    (s"Can ${p3.name} reach the president of ${p3.country} by common connections?", PersonIdentifier(p3.id), Country(p3.country), None, List.empty[Person]),
    (s"Can ${p4.name} reach the president of ${p4.country} by common connections?", PersonIdentifier(p4.id), Country(p4.country), None, List.empty[Person]),
    (s"Can ${p6.name} reach the president of ${p6.country} by common connections?", PersonIdentifier(p6.id), Country(p6.country), Some(p9), List(p6, p1, p2, p8, p9)),
    (s"Can ${p7.name} reach the president of ${p7.country} by common connections?", PersonIdentifier(p7.id), Country(p7.country), Some(p9), List(p7, p1, p2, p8, p9))
  )

  val followersSinceDateExample = Table(
    ("description", "personId", "date", "expected"),
    (s"${p1.name}'s followers since January 2017", PersonIdentifier(p1.id), Instant.parse("2017-01-01T00:00:00.00Z"), List(p6, p7)),
    (s"${p3.name}'s followers since January 2015", PersonIdentifier(p3.id), Instant.parse("2015-01-01T00:00:00.00Z"), List(p1)),
    (s"${p3.name}'s followers since January 2014", PersonIdentifier(p3.id), Instant.parse("2014-01-01T00:00:00.00Z"), List(p1, p2))
  )

  val firstFollowersOfExample = Table(
    ("description", "personId", "expected"),
    (s"What's the first follower of whom ${p4.name} has first started following?", PersonIdentifier(p4.id), Option((p6, p1))),
    (s"What's the first follower of whom ${p1.name} has first started following?", PersonIdentifier(p1.id), Option((p6, p1))),
    (s"What's the first follower of whom ${p2.name} has first started following?", PersonIdentifier(p2.id), Option((p8, p2))),
    (s"What's the first follower of whom ${p5.name} has first started following?", PersonIdentifier(p5.id), None)
  )


}
