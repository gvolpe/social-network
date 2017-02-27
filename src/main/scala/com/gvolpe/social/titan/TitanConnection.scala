package com.gvolpe.social.titan

import com.thinkaurelius.titan.core.TitanFactory
import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.slf4j.LoggerFactory

trait TitanConnection {
  val log = LoggerFactory.getLogger("TitanConnection")

  // See http://s3.thinkaurelius.com/docs/titan/1.0.0/indexes.html
  protected def createIndices(graph: TitanGraph): Unit = {
    graph.tx().rollback() //Never create new indexes while a transaction is active
    val mgmt = graph.openManagement()
    val personIdKey   = mgmt.getOrCreatePropertyKey("personId")
    val personNameKey = mgmt.getOrCreatePropertyKey("personName")
    mgmt.buildIndex("byPersonId", classOf[Vertex]).addKey(personIdKey).buildCompositeIndex()
    mgmt.buildIndex("byPersonName", classOf[Vertex]).addKey(personNameKey).buildCompositeIndex()
    mgmt.commit()
    graph.tx().commit()
  }

  def g: ScalaGraph[TitanGraph]
}

object SocialNetworkTitanConfiguration {
  val PersonId          = Key[Long]("personId")
  val PersonName        = Key[String]("personName")
  val PersonAge         = Key[Int]("personAge")
  val PersonCountry     = Key[String]("personCountry")
  val PersonProfession  = Key[String]("personProfession")

  val TimestampKey      = Key[Long]("timestamp")

  val PersonLabel       = "person"
  val Following         = "following"
  val FollowedBy        = "followedBy"
}

trait TitanCassandraConnection extends TitanConnection {

  private def connect(): TitanGraph = {
    log.info("Connecting to Titan:Db Cassandra...")
    val graph = TitanFactory.build()
      .set("storage.backend","cassandra")
      .set("storage.hostname","127.0.0.1")
      .open()
    createIndices(graph)
    graph
  }

  override val g = connect().asScala
}

trait TitanInMemoryConnection extends TitanConnection {

  private def connect(): TitanGraph = {
    log.info("Connecting to Titan:Db in memory...")
    val graph = TitanFactory.build()
      .set("storage.backend","inmemory")
      .open()
    createIndices(graph)
    graph
  }

  override val g = connect().asScala
}

