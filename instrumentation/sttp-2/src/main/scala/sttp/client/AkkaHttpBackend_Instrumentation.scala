/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package sttp.client

import akka.event.LoggingAdapter
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.nr.agent.instrumentation.sttp.DelegateFuture
import sttp.client.akkahttp.AkkaHttpBackend.EncodingHandler

import scala.concurrent.{ExecutionContext, Future}


@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "sttp.client.akkahttp.AkkaHttpBackend")
private class AkkaHttpBackend_Instrumentation {
  def apply(
             options: SttpBackendOptions,
             customHttpsContext: Option[HttpsConnectionContext],
             customConnectionPoolSettings: Option[ConnectionPoolSettings],
             customLog: Option[LoggingAdapter],
             customizeRequest: HttpRequest => HttpRequest,
             customizeWebsocketRequest: WebSocketRequest => WebSocketRequest,
             customEncodingHandler: EncodingHandler
           )(implicit
             ec: ExecutionContext = ExecutionContext.global
           ): SttpBackend[Future, Source[ByteString, Any], Any] = new DelegateFuture(Weaver.callOriginal())
}
