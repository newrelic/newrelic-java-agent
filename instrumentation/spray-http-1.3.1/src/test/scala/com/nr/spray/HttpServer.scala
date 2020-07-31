/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.spray

import akka.actor.{Props, ActorSystem}
import akka.event.Logging
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.Route

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HttpServer(val routes: Route = RouteService.defaultRoute) {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val timeout: Timeout = 3 seconds

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val handler = system.actorOf(Props(new RouteService(routes)), name = "handler")

  def start(port: Int) = Await.ready(
    IO(Http) ? Http.Bind(handler, "localhost", port),
    timeout.duration
  )

  def stop() = {
    IO(Http) ? Http.CloseAll
    system.stop(handler)
  }
}