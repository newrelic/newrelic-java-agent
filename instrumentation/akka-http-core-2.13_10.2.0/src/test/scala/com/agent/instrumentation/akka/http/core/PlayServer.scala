/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.akka.http.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, get, onSuccess, path}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, _}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

//how play 2.6 sets up a server
class PlayServer() {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout: Timeout = 3 seconds

  var bindingFuture: Future[Http.ServerBinding] = _

  def start(port: Int, async: Boolean) = {
    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(GET, Uri.Path("/ping"), _, _, _) => HttpResponse(entity = "Boops!")
    }
    val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
      case HttpRequest(GET, Uri.Path("/asyncPing"), _, _, _) => Future(HttpResponse(entity = "Hoops!"))
    }
    bindingFuture = if (async)
      Http().bindAndHandleAsync(asyncRequestHandler, interface = "localhost", port)
    else
      Http().bindAndHandleSync(requestHandler, interface = "localhost", port)
    Await.ready(bindingFuture, timeout.duration)
  }

  def startFromFlow(port: Int) = {
    val routeFlow =
      path("ping") {
        get(onSuccess(Future("Hoops"))(complete(_)))
      }

    bindingFuture = Http().newServerAt("localhost", port).bindFlow(routeFlow)
    Await.ready(bindingFuture, timeout.duration)
  }

  def stop() = {
    if (bindingFuture != null) {
      bindingFuture.flatMap(_.unbind()).onComplete(_ => {
        system.terminate()
      })
    }
  }
}
