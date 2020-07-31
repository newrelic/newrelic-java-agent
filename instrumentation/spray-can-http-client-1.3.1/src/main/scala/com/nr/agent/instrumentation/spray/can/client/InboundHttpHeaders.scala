/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spray.can.client

import java.util

import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import spray.http.{HttpHeader, HttpResponse}

import scala.collection.JavaConversions

class InboundHttpHeaders(originalResponse: HttpResponse) extends ExtendedInboundHeaders {
  override def getHeader(name: String): String = {
    for (header: HttpHeader <- originalResponse.headers) {
      if (header.name.equals(name)) {
        return header.value
      }
    }
    return null
  }

  override def getHeaders(name: String): util.List[String] = {
    val headers = originalResponse.headers.filter(header => header.is(name.toLowerCase)).map(header => header.value)
    if (headers.isEmpty) {
      return null
    }
    JavaConversions.seqAsJavaList(headers)
  }

  override def getHeaderType(): HeaderType = {
    return HeaderType.HTTP
  }
}
