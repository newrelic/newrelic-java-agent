/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http.core

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.Logging
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.stream.scaladsl.{Source, _}
import org.apache.pekko.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

//how the pekko http core docs' example sets up a server
class PekkoServer() {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher

  implicit val timeout: Timeout = 3 seconds

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  var serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] = _
  var bindingFuture: Future[Http.ServerBinding] = _

  def start(port: Int, async: Boolean) = {

    serverSource = Http().newServerAt(interface = "localhost", port).connectionSource()

    if (async) {

      val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(GET, Uri.Path("/asyncPing"), _, _, _) => Future[HttpResponse](HttpResponse(entity = "Hoops!"))
      }

      bindingFuture = serverSource.to(Sink.foreach {
        connection =>
          println("accepted connection from: " + connection.remoteAddress)
          connection handleWithAsyncHandler asyncRequestHandler
      }).run()
    }
    else {

      val requestHandler: HttpRequest => HttpResponse = {
        case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
          HttpResponse(entity = "Hoops!")
      }

      bindingFuture = serverSource.to(Sink.foreach {
        connection =>
          println("accepted connection from: " + connection.remoteAddress)
          connection handleWithSyncHandler requestHandler
      }).run()
    }

    Await.ready({
      bindingFuture
    }, timeout.duration)
  }

  def stop() = {
    if (bindingFuture != null) {
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }
}
