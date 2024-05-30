/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekkohttpcore

import java.util.logging.Level

import org.apache.pekko.http.scaladsl.model.HttpResponse
import com.newrelic.agent.bridge.{AgentBridge, Token}
import com.newrelic.api.agent.{NewRelic, Transaction}
import com.newrelic.api.agent.weaver.Weaver

import scala.concurrent.{ExecutionContext, Future}

object ResponseFuture {

  def wrapResponse(token: Token)(implicit ec: ExecutionContext): (HttpResponse) => Future[HttpResponse] = { response:HttpResponse => {
    Future {
      var updatedResponse: HttpResponse = response
      var localToken = token

      try {
        val txn: Transaction = localToken.getTransaction
        if (txn != null) {
          val wrappedResponse = new ResponseWrapper(response)
          txn.setWebResponse(wrappedResponse)
          txn.addOutboundResponseHeaders()
          txn.markResponseSent()
          updatedResponse = wrappedResponse.response

          localToken.expire()
          localToken = null
        }
      } catch {
        case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
          try {
            localToken.expire()
            localToken = null;
          }
          catch {
            case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
          }

      }

      updatedResponse
    }
  }
  }
}
