/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akkahttpcore

import java.net.URI
import java.util.concurrent.Executors

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{HttpParameters, Segment}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object AkkaHttpUtils {

  implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  def finishSegmentOnComplete(httpRequest: HttpRequest, httpResponseFuture: Future[HttpResponse], segment: Segment): Unit = {
    httpResponseFuture onComplete {
      case Success(response) =>
        try {
          segment.reportAsExternal(HttpParameters
            .library("AkkaHttpClient")
            .uri(new URI(httpRequest.uri.toString()))
            .procedure(httpRequest.method.value)
            .inboundHeaders(new AkkaHttpInboundHeaders(httpRequest))
            .build())
          segment.end()
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }

      case Failure(t) =>
        try {
          // In the case of an error, just end the segment.
          // We probably don't want to report an error here because it may just be a 404
          segment.end()
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }
    }
  }
}
