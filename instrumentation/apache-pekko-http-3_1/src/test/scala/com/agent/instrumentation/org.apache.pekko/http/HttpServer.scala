/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.Logging
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{RequestContext, Route}
import org.apache.pekko.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.language.postfixOps

class HttpServer(val routes: Route = RouteService.defaultRoute) {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 60 seconds

  val config = ConfigFactory.load()
  val logger = Logging(system, "hallo")

  var handle: Future[ServerBinding] = _

  def start(port: Int) = {
    Await.ready({
      handle = Http().newServerAt("localhost", port).bind(routes)
      handle
    }, timeout.duration)
  }

  def stop() = {
    if (handle != null) {
      handle.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }
}

object RouteService {
  val defaultRoute = {
    path("test") {
      get { (ctx: RequestContext) =>
        ctx.complete("FAIL")
      }
    }
  }
}
