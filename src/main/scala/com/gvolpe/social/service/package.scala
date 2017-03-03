package com.gvolpe.social

import com.gvolpe.social.model.Person
import com.gvolpe.social.titan.SocialNetworkTitanConfiguration._
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Path

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

  implicit class PathOps(path: Path) {
    import scala.collection.JavaConversions._

    def persons: List[Person] = {
      path.objects.toList.collect {
        case v: Vertex => v.mapPerson
      }
    }
  }

}
