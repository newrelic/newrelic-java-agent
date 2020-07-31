/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws

import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import play.api.libs.ws.StandaloneWSResponse

import scala.collection.JavaConversions

class InboundWrapper(var response: StandaloneWSResponse) extends ExtendedInboundHeaders {

  override def getHeaderType = {
    HeaderType.HTTP
  }

  override def getHeader(name: String) = {
    response.header(name).orNull
  }

  override def getHeaders(name: String): java.util.List[String] = {
    val result = response.headerValues(name)
    if (result.isEmpty) {
      return null
    }
    JavaConversions.seqAsJavaList(result)
  }

}
