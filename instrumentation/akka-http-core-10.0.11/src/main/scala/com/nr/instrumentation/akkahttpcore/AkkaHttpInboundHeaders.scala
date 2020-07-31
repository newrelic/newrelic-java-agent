/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akkahttpcore

import java.util

import akka.http.scaladsl.model.HttpRequest
import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}

import scala.collection.JavaConversions

class AkkaHttpInboundHeaders(val httpRequest: HttpRequest) extends ExtendedInboundHeaders {

  override def getHeaderType: HeaderType = HeaderType.HTTP

  override def getHeader(name: String): String = {
    val header = httpRequest.getHeader(name)
    if (!header.isPresent) {
      return null
    }
    header.get().value()
  }

  override def getHeaders(name: String): util.List[String] = {
    val headers = httpRequest.headers.filter(header => header.is(name.toLowerCase)).map(header => header.value)
    if (headers.isEmpty) {
      return null
    }
    JavaConversions.seqAsJavaList(headers)
  }
}
