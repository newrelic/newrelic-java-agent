/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http

import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import com.newrelic.api.agent.{ExtendedResponse, HeaderType}

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

  def getHeaderType: HeaderType = {
    HeaderType.HTTP
  }

  def setHeader(name: String, value: String): Unit = {
    response = response.addHeader(new RawHeader(name, value))
  }

  def getContentLength: Long = {
    val contentLength = response.getHeader("Content-Length")
    if (contentLength.isPresent) {
      return contentLength.get().value().toLong
    }
    response.entity.getContentLengthOption().orElse(-1L)
  }

}
