/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.newrelic.agent.bridge.{AgentBridge, Token, TransactionNamePriority}
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{NewRelic, Trace}
import com.nr.instrumentation.pekkohttpcore.{RequestWrapper, ResponseFuture}

import scala.concurrent.{ExecutionContext, Future}
import scala.runtime.AbstractFunction1

class AsyncRequestHandler(handler: HttpRequest => Future[HttpResponse])(implicit ec: ExecutionContext) extends AbstractFunction1[HttpRequest, Future[HttpResponse]] {

  val transactionCategory: String = "PekkoHttpCore"

  @Trace(dispatcher = true)
  override def apply(param: HttpRequest): Future[HttpResponse] = {

    var futureResponse: Future[HttpResponse] = null
    var token: Token = null

    try {
      token = AgentBridge.getAgent.getTransaction.getToken
      AgentBridge.getAgent.getTransaction.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, transactionCategory, "pekkoHandler")
      NewRelic.getAgent.getTracedMethod.setMetricName("Pekko", "RequestHandler")

      val wrappedRequest: RequestWrapper = new RequestWrapper(param)
      NewRelic.getAgent().getTransaction().setWebRequest(wrappedRequest)
    } catch {
      case t: Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle())
      }
    }

    futureResponse = handler.apply(param)

    try {
      // Modify the original response by passing it through our map function (since a copy
      // is required due to the response headers being immutable). Return the (future) result of this map function.
      futureResponse.flatMap(ResponseFuture.wrapResponse(token))
    } catch {
      case t: Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle())
        futureResponse
      }
    }
  }
}
