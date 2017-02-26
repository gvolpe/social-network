package com.gvolpe.social.service

import com.gvolpe.social.model.{Friendship, Person, PersonIdentifier}
import com.gvolpe.social.titan.TitanInMemoryConnection
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

// TODO: Add failure scenarios like follow someone that does not exist or an existent follower
class SocialNetworkServiceSpec extends SocialNetworkServiceFixture {

  override def afterAll() = {
    println("Closing Titan In Memory connection...")
    TestSocialNetworkService.g.close()
  }

  forAll(createPersonsExample) { (description, person) =>
    it should description in {
      TestSocialNetworkService.createPerson(person) should be (Some(person))
    }
  }

  forAll(createFollowersExample) { (description, from, to) =>
    it should description in {
      TestSocialNetworkService.follow(from, to) should be (Some(Friendship(from, to)))
    }
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
                                  with BeforeAndAfterAll
                                  with PropertyChecks {

  object TestSocialNetworkService extends SocialNetworkService with TitanInMemoryConnection

  val p1 = Person(1, "Gabi")
  val p2 = Person(2, "Damian")
  val p3 = Person(3, "John")
  val p4 = Person(4, "Mike")
  val p5 = Person(5, "Chris")

  val createPersonsExample = Table(
    ("description", "person"),
    ("create person 1", p1),
    ("create person 2", p2),
    ("create person 3", p3),
    ("create person 4", p4),
    ("create person 5", p5)
  )

  val createFollowersExample = Table(
    ("description", "from", "to"),
    ("Gabi follows Damian", p1, p2),
    ("Gabi follows John", p1, p3),
    ("Damian follows Gabi", p2, p1),
    ("Damian follows John", p2, p3),
    ("John follows Chris", p3, p5),
    ("Mike follows Gabi", p4, p1)
  )

  val followingExample = Table(
    ("description", "person", "expected"),
    ("Gabi is following", PersonIdentifier(p1.id), List(p2, p3)),
    ("Damian is following", PersonIdentifier(p2.id), List(p1, p3)),
    ("John is following", PersonIdentifier(p3.id), List(p5)),
    ("Mike is following", PersonIdentifier(p4.id), List(p1))
  )

  val followedByExample = Table(
    ("description", "person", "expected"),
    ("Gabi is followed by", PersonIdentifier(p1.id), List(p2, p4)),
    ("Damian is followed by", PersonIdentifier(p2.id), List(p1)),
    ("John is followed by", PersonIdentifier(p3.id), List(p1, p2)),
    ("Chris is followed by", PersonIdentifier(p5.id), List(p3))
  )

}
