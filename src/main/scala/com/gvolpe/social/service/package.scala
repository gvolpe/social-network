package com.gvolpe.social

import com.gvolpe.social.model.Person
import com.gvolpe.social.titan.SocialNetworkTitanConfiguration._
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Path

package object service {

  implicit class VertexOps(v: Vertex) {
    def mapPerson: Person = Person(
      v.value2(PersonId),
      v.value2(PersonName),
      v.value2(PersonAge),
      v.value2(PersonCountry),
      v.value2(PersonProfession)
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
