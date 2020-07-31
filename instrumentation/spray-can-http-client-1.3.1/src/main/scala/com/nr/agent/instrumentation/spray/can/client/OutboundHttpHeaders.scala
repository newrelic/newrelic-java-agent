/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spray.can.client

import java.net.InetSocketAddress

import com.newrelic.api.agent.{HeaderType, NewRelic, OutboundHeaders, Segment}
import spray.http.{HttpHeaders, HttpRequest_Instrumentation}

/**
  * Spray's HttpRequest is immutable so we have to create a copy with the new headers.
  */
class OutboundHttpHeaders(originalRequest: HttpRequest_Instrumentation) extends OutboundHeaders {
  private var request: HttpRequest_Instrumentation = originalRequest

  override def getHeaderType() :HeaderType = {
    HeaderType.HTTP
  }

  override def setHeader(name: String, value: String) :Unit = {
    request = request.withHeaders(request.headers ++ List(HttpHeaders.RawHeader(name, value)))
  }

  def getRequest(segment: Segment, remoteAddress: InetSocketAddress, isSSL: Boolean): HttpRequest_Instrumentation = {
    request.headersAdded = true
    request.segment = segment
    request.remoteAddress = remoteAddress
    request.isSSL = isSSL
    request
  }
}
