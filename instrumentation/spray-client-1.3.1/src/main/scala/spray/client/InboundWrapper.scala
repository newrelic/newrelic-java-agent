/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sprayclient

import java.util

import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import spray.http.HttpResponse

import scala.collection.JavaConversions

class InboundWrapper(response: HttpResponse) extends ExtendedInboundHeaders {

  def getHeaderType: HeaderType = {
    HeaderType.HTTP
  }

  def getHeader(name: String): String = {
    response.headers.find(header => header.is(name.toLowerCase)).map(header => header.value).orNull
  }

  override def getHeaders(name: String): util.List[String] = {
    val headers = response.headers.filter(header => header.is(name.toLowerCase)).map(header => header.value)
    if (headers.isEmpty) {
      return null
    }
    JavaConversions.seqAsJavaList(headers)
  }

}
