package com.gvolpe.social

import com.gvolpe.social.model.Person
import com.gvolpe.social.titan.SocialNetworkTitanConfiguration._
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Path

import scala.util.{Failure, Success, Try}

package object service {

  implicit class VertexOps(p: Vertex) {
    def mapPerson: Person = Person(
      p.value2(PersonId),
      p.value2(PersonName),
      p.value2(PersonAge),
      p.value2(PersonCountry),
      p.value2(PersonProfession)
    )
  }

  def personFromPath(path: Path, index: Int): List[Person] = {
    Try(path.get[Vertex](index)) match {
      case Success(v) => List(v.mapPerson)
      case Failure(t) => List.empty[Person]
    }
  }

}
