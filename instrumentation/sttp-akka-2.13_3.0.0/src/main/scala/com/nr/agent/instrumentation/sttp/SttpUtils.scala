/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.newrelic.api.agent.{HttpParameters, NewRelic, Segment, TransactionNamePriority}
import sttp.client3.{Request, Response}

import java.net.URI


object SttpUtils {

  def startSegment[R, T](request: Request[T, R]): Segment = {
    val segment = NewRelic.getAgent.getTransaction.startSegment("SttpBackend", "send")
    segment.addOutboundRequestHeaders(new OutboundHttpHeaders(request))
    segment
  }

  def finishSegment[R, T](request: Request[T, R], segment: Segment, response: Response[T]): Unit = {
    segment.reportAsExternal(HttpParameters
      .library("Sttp")
      .uri(new URI(request.uri.toString()))
      .procedure(request.method.method)
      .inboundHeaders(new InboundHttpHeaders(response.headers))
      .build())
    segment.end()
  }
}
