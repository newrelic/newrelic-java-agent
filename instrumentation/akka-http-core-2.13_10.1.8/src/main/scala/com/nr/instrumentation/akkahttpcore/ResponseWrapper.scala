/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akkahttpcore

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.RawHeader
import com.newrelic.agent.bridge.{AgentBridge, Token}
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{ExtendedResponse, HeaderType, Transaction}

import scala.concurrent.{ExecutionContext, Future}

class ResponseWrapper(var response: HttpResponse) extends ExtendedResponse {

  def getStatus: Int = {
    response.status.intValue
  }

  def getStatusMessage: String = {
    response.status.reason
  }

  def getContentType: String = {
    response.entity.contentType.value
  }

  override def getHeaderType = HeaderType.HTTP

  override def setHeader(name: String, value: String): Unit = {
    response = response.addHeader(new RawHeader(name, value))
  }

  def getContentLength: Long = {
    response.entity.contentLengthOption.getOrElse(0L)
  }
}
object ResponseWrapper {
  def wrapAsyncResponse(token: Token)(implicit ec: ExecutionContext): HttpResponse => Future[HttpResponse] = {
    response: HttpResponse => Future(wrapResponse(token, response))
  }


  def wrapResponse(token: Token, response: HttpResponse): HttpResponse = {
    val localToken = token

    try {
      val txn: Transaction = localToken.getTransaction
      if (txn != null) {
        val wrappedResponse = new ResponseWrapper(response)
        txn.setWebResponse(wrappedResponse)
        txn.addOutboundResponseHeaders()
        txn.markResponseSent()
        val updatedResponse = wrappedResponse.response
        localToken.expire()
        updatedResponse
      } else {
        response
      }

    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        try {
          localToken.expire()
          response
        }
        catch {
          case t: Throwable =>
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
            response
        }
    }
  }
}