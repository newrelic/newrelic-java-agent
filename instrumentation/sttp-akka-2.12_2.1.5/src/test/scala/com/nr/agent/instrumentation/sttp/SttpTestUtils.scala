/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.newrelic.agent.introspec.internal.HttpServerRule
import com.newrelic.agent.introspec.{Introspector, TraceSegment, TransactionTrace}
import com.newrelic.api.agent.Trace
import sttp.client.{NothingT, Response, SttpBackend, UriContext, basicRequest}

import collection.JavaConverters._

object SttpTestUtils {

  @Trace(dispatcher = true)
  def makeRequest[F[_]](implicit backend: SttpBackend[F, Nothing, NothingT], server: HttpServerRule): F[Response[Either[String, String]]] = {
    basicRequest.get(uri"${server.getEndPoint}?no-transaction=1").send()
  }

  def getTraces()(implicit introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  def getSegments(traces : Iterable[TransactionTrace]): Iterable[TraceSegment] =
    traces.flatMap(trace => this.getSegments(trace.getInitialTraceSegment))

  private def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }
}
