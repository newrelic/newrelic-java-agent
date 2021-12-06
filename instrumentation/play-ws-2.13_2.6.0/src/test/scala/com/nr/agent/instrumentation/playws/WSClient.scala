/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.{StandaloneWSRequest, WSRequestExecutor, WSRequestFilter}
import play.api.libs.ws.ahc._

import scala.util.{Failure, Success}

object WSClient {
  import scala.concurrent.ExecutionContext.Implicits._

  def makeRequest(url: String, responseRunnable: ResponseRunnable): Unit = {
    // Create Akka system for thread and streaming management
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    // Create the standalone WS client
    // no argument defaults to a AhcWSClientConfig created from
    // "AhcWSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)"
    val wsClient = StandaloneAhcWSClient()

    wsClient.url(url).get()
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
      .onComplete({
        case Success(result) => {
          responseRunnable.onResponse(result)
        }
        case Failure(t) => t.printStackTrace()
      })
  }

  def makeRequestWithHeaders(url: String, responseRunnable: ResponseRunnable): Unit = {
    // Create Akka system for thread and streaming management
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    // Create the standalone WS client
    // no argument defaults to a AhcWSClientConfig created from
    // "AhcWSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)"
    val wsClient = AhcWSClient()

    wsClient.url(url).withHeaders(("TestHeader", "TestValue")).get()
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
      .onComplete({
        case Success(result) => {
          responseRunnable.onResponse(result)
        }
        case Failure(t) => t.printStackTrace()
      })
  }

  def makeRequestWithFilterHeaders(url: String, responseRunnable: ResponseRunnable): Unit = {
    // Create Akka system for thread and streaming management
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    // Create the standalone WS client
    // no argument defaults to a AhcWSClientConfig created from
    // "AhcWSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)"
    val wsClient = AhcWSClient()

    wsClient.url(url)
      .withRequestFilter(new TestRequestFilter)
      .withHeaders(("TestHeader", "TestValue"))
      .get()
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
      .onComplete({
        case Success(result) => {
          responseRunnable.onResponse(result)
        }
        case Failure(t) => t.printStackTrace()
      })
  }

  class TestRequestFilter() extends WSRequestFilter {
    def apply(executor: WSRequestExecutor): WSRequestExecutor = {
      WSRequestExecutor { request: StandaloneWSRequest =>
        executor(request.addHttpHeaders(("X-Request-ID", "TEST")))
      }
    }
  }
}
