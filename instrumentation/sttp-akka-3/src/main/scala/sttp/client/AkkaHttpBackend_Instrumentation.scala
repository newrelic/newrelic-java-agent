/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package sttp.client

import akka.event.LoggingAdapter
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.nr.agent.instrumentation.sttp.DelegateFuture
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.client3.akkahttp.AkkaHttpBackend.EncodingHandler
import sttp.client3.{SttpBackend, SttpBackendOptions}

import scala.concurrent.{ExecutionContext, Future}

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "sttp.client3.akkahttp.AkkaHttpBackend")
class AkkaHttpBackend_Instrumentation {
  def apply(
             options: SttpBackendOptions,
             customHttpsContext: Option[HttpsConnectionContext],
             customConnectionPoolSettings: Option[ConnectionPoolSettings],
             customLog: Option[LoggingAdapter],
             customizeRequest: HttpRequest => HttpRequest,
             customizeWebsocketRequest: WebSocketRequest => WebSocketRequest,
             customizeResponse: (HttpRequest, HttpResponse) => HttpResponse,
             customEncodingHandler: EncodingHandler
           )(implicit
             ec: Option[ExecutionContext]
           ): SttpBackend[Future, AkkaStreams with WebSockets] = new DelegateFuture(Weaver.callOriginal())
}
