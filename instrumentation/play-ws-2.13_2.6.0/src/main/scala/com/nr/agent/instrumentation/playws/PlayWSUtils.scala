/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws

import java.util.concurrent.Executors

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{HttpParameters, NewRelic, Segment}
import play.api.libs.ws.{StandaloneWSRequest, StandaloneWSResponse}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object PlayWSUtils {

  implicit lazy val global: ExecutionContextExecutor = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  def createSeq: mutable.Buffer[(String, String)] = {
    mutable.ArrayBuffer()
  }

  def start(): Segment = {
    var segment: Segment = null
    try {
      segment = NewRelic.getAgent.getTransaction.startSegment("External", "PlayWS")
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        try {
          if (segment != null) {
            segment.end()
            segment = null
          }
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }
    }
    segment
  }

  def finish(segment: Segment, procedure: String, request: StandaloneWSRequest, response: Future[StandaloneWSResponse]): Future[StandaloneWSResponse] = {
    if (segment == null) {
      return response
    }
    var localSegment = segment
    localSegment.setMetricName("External", "PlayWS", if (request.method != null) request.method.toLowerCase else procedure)

    tryCatchWrapper({
      response onComplete {
        case Success(result) =>
          tryCatchWrapper({
            localSegment.reportAsExternal(
              HttpParameters
                .library("PlayWS")
                .uri(request.uri)
                .procedure(if (request.method != null) request.method.toLowerCase else procedure)
                .inboundHeaders(new InboundWrapper(result))
                .build())
            localSegment.end()
            localSegment = null
          }, segment)

        case Failure(t) => {
          tryCatchWrapper({
            localSegment.reportAsExternal(
              HttpParameters
                .library("PlayWS")
                .uri(request.uri)
                .procedure(if (request.method != null) request.method.toLowerCase else procedure)
                .noInboundHeaders()
                .build())
            localSegment.end()
            localSegment = null
          }, segment)
        }
      }

    }, segment)

    response
  }


  def tryCatchWrapper(func: => Any, segment: Segment): Unit = {
    try {
      func
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        try {
          if (segment != null) {
            segment.end()
          }
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }
    }
  }
}
