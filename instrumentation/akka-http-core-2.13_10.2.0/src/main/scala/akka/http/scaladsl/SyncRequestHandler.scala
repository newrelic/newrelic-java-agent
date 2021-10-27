/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.newrelic.agent.bridge.{AgentBridge, TransactionNamePriority}
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{NewRelic, Trace, Transaction}
import com.nr.instrumentation.akkahttpcore.{RequestWrapper, ResponseWrapper}

import scala.runtime.AbstractFunction1

class SyncRequestHandler(handler: HttpRequest => HttpResponse) extends AbstractFunction1[HttpRequest, HttpResponse] {

  val transactionCategory :String = "AkkaHttpCore"

  @Trace(dispatcher = true)
  override def apply(param: HttpRequest): HttpResponse = {

    try {
      AgentBridge.getAgent.getTransaction.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, transactionCategory, "akkaHandler")
      NewRelic.getAgent.getTracedMethod.setMetricName("Akka", "RequestHandler")

      val wrappedRequest: RequestWrapper = new RequestWrapper(param)
      NewRelic.getAgent().getTransaction().setWebRequest(wrappedRequest)

    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
    }

    val response: HttpResponse = handler.apply(param)

    try {

      var updatedResponse: HttpResponse = response

      val txn: Transaction = NewRelic.getAgent().getTransaction()
      
      if (txn != null) {
        val wrappedResponse = new ResponseWrapper(response)
        txn.setWebResponse(wrappedResponse)
        txn.addOutboundResponseHeaders()
        txn.markResponseSent()
        updatedResponse = wrappedResponse.response
      }
      
      updatedResponse
      
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        response
    }
  }
}
