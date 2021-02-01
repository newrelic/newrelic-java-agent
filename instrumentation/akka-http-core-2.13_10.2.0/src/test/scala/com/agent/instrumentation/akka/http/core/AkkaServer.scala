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
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, _}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

//how the akka http core docs' example sets up a server
class AkkaServer() {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout: Timeout = 3 seconds

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  var serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] = _
  var bindingFuture: Future[Http.ServerBinding] = _

  def start(port: Int, async: Boolean) = {

    serverSource = Http().bind(interface = "localhost", port)

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
