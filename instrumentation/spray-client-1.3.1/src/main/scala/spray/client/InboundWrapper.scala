/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sprayclient

import java.util
import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import spray.http.{HttpHeader}

import scala.collection.JavaConversions

class InboundWrapper(headers: List[HttpHeader]) extends ExtendedInboundHeaders {

  def getHeaderType: HeaderType = {
    HeaderType.HTTP
  }

  def getHeader(name: String): String = {
    headers.find(header => header.is(name.toLowerCase)).map(header => header.value).orNull
  }

  override def getHeaders(name: String): util.List[String] = {
    val tmpHeaders = headers.filter(header => header.is(name.toLowerCase)).map(header => header.value)
    if (tmpHeaders.isEmpty) {
      return null
    }
    JavaConversions.seqAsJavaList(tmpHeaders)
  }

}
