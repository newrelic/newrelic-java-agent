/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http

import org.apache.pekko.http.scaladsl.model.HttpRequest
import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}

import java.util
import scala.jdk.javaapi.CollectionConverters

class InboundWrapper(request: HttpRequest) extends ExtendedInboundHeaders {

  def getHeaderType: HeaderType = {
    HeaderType.HTTP
  }

  def getHeader(name: String): String = {
    request.headers.find(header => header.is(name.toLowerCase)).map(header => header.value).orNull
  }

  override def getHeaders(name: String): util.List[String] = {
    val headers = request.headers.filter(header => header.is(name.toLowerCase)).map(header => header.value)
    if (headers.isEmpty) {
      return null
    }
    CollectionConverters.asJava(headers)
  }

}

