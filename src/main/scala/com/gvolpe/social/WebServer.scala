package com.gvolpe.social

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server, ServerApp}

import scalaz.concurrent.Task

object WebServer extends ServerApp {

  override def server(args: List[String]): Task[Server] =
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .start

}
