package com.gvolpe.social.service

import java.time.Instant

import com.gvolpe.social.model.{Person, PersonIdentifier}
import com.gvolpe.social.titan.TitanInMemoryConnection
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}

// TODO: Add failure scenarios like follow someone that does not exist or an existent follower
class SocialNetworkServiceSpec extends SocialNetworkServiceFixture {

//  override def beforeEach() = {
//    createPersonsAndConnections()
//  }

  override def beforeAll() = {
    createPersonsAndConnections()
  }

  override def afterAll() = {
    println("Closing Titan In Memory connection...")
    TestSocialNetworkService.g.close()
  }

  forAll(followingExample) { (description, personId, expected) =>
    it should description in {
      TestSocialNetworkService.findFollowing(personId).sortBy(_.id) should be (expected)
    }
  }

  forAll(followedByExample) { (description, personId, expected) =>
    it should description in {
      TestSocialNetworkService.findFollowers(personId).sortBy(_.id) should be (expected)
    }
  }

}

trait SocialNetworkServiceFixture extends FlatSpecLike
                                  with Matchers
                                  with BeforeAndAfterEach
                                  with BeforeAndAfterAll
                                  with PropertyChecks {

  object TestSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

  val p1 = Person(1, "Gabi", 29, "Argentina", "Software Engineer")
  val p2 = Person(2, "Damian", 29, "Argentina", "Software Engineer")
  val p3 = Person(3, "John", 30, "Ireland", "Pilot")
  val p4 = Person(4, "Mike", 32, "England", "Architect")
  val p5 = Person(5, "Chris", 22, "Germany", "Marketing")
  val p6 = Person(6, "Guille", 24, "Argentina", "Sales")
  val p7 = Person(7, "Ivan", 21, "Argentina", "Student")
  val p8 = Person(8, "Gustavo", 35, "Argentina", "Lawyer")
  val p9 = Person(9, "Capusotto", 57, "Argentina", "President")

  def createPersonsAndConnections() = {
    TestSocialNetworkService.createPerson(p1)
    TestSocialNetworkService.createPerson(p2)
    TestSocialNetworkService.createPerson(p3)
    TestSocialNetworkService.createPerson(p4)
    TestSocialNetworkService.createPerson(p5)
    TestSocialNetworkService.createPerson(p6)
    TestSocialNetworkService.createPerson(p7)
    TestSocialNetworkService.createPerson(p8)
    TestSocialNetworkService.createPerson(p9)

    TestSocialNetworkService.follow(p1, p2, Instant.parse("2015-07-01T00:00:00.00Z")) // Gabi --> Damian
    TestSocialNetworkService.follow(p1, p3, Instant.parse("2015-11-25T00:00:00.00Z")) // Gabi --> John
    TestSocialNetworkService.follow(p1, p6, Instant.parse("2007-01-23T00:00:00.00Z")) // Gabi --> Guille
    TestSocialNetworkService.follow(p2, p1, Instant.parse("2015-08-14T00:00:00.00Z")) // Damian --> Gabi
    TestSocialNetworkService.follow(p2, p3, Instant.parse("2014-10-20T00:00:00.00Z")) // Damian --> John
    TestSocialNetworkService.follow(p2, p8, Instant.parse("2009-05-18T00:00:00.00Z")) // Damian --> Gustavo
    TestSocialNetworkService.follow(p3, p5, Instant.parse("2010-05-17T00:00:00.00Z")) // John --> Chris
    TestSocialNetworkService.follow(p4, p1, Instant.parse("2012-03-07T00:00:00.00Z")) // Mike --> Gabi
    TestSocialNetworkService.follow(p4, p6, Instant.parse("2010-07-01T00:00:00.00Z")) // Mike --> Guille
    TestSocialNetworkService.follow(p6, p1, Instant.parse("2017-02-27T00:00:00.00Z")) // Guille --> Gabi
    TestSocialNetworkService.follow(p7, p1, Instant.parse("2017-03-01T00:00:00.00Z")) // Ivan --> Gabi
    TestSocialNetworkService.follow(p8, p9, Instant.parse("2008-06-13T00:00:00.00Z")) // Gustavo --> Capusotto
    TestSocialNetworkService.follow(p9, p8, Instant.parse("2016-08-02T00:00:00.00Z")) // Capusotto --> Gustavo
  }

  val followingExample = Table(
    ("description", "person", "expected"),
    ("Gabi is following", PersonIdentifier(p1.id), List(p2, p3, p6)),
    ("Damian is following", PersonIdentifier(p2.id), List(p1, p3, p8)),
    ("John is following", PersonIdentifier(p3.id), List(p5)),
    ("Mike is following", PersonIdentifier(p4.id), List(p1, p6))
  )

  val followedByExample = Table(
    ("description", "person", "expected"),
    ("Gabi is followed by", PersonIdentifier(p1.id), List(p2, p4, p6, p7)),
    ("Damian is followed by", PersonIdentifier(p2.id), List(p1)),
    ("John is followed by", PersonIdentifier(p3.id), List(p1, p2)),
    ("Chris is followed by", PersonIdentifier(p5.id), List(p3))
  )

}
