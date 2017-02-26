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
    val friendKey = mgmt.getOrCreatePropertyKey("friendId")
    mgmt.buildIndex("byFriendId", classOf[Vertex]).addKey(friendKey).buildCompositeIndex()
    mgmt.commit()
    graph.tx().commit()
  }

  def g: ScalaGraph[TitanGraph]
}

trait SocialNetworkTitanConfiguration {
  self: TitanConnection =>

  protected val FriendId      = Key[Long]("friendId")
  protected val FriendLabel   = "friend"
  protected val FriendLink    = "friend-link"
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

