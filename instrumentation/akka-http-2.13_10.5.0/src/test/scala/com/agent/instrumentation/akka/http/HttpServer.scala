/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.akka.http

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class HttpServer(val routes: Route = RouteService.defaultRoute) {
  implicit val system = ActorSystem("System")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout: Timeout = 60 seconds

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  var handle: Future[ServerBinding] = _

  def start(port: Int): Future[ServerBinding] = {
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
