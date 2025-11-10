/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekkohttpcore

import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import com.newrelic.api.agent.{HeaderType, ExtendedResponse}

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
