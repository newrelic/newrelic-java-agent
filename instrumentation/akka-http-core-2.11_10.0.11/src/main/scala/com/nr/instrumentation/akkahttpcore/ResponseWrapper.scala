/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akkahttpcore

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.RawHeader
import com.newrelic.api.agent.{HeaderType, Response}

class ResponseWrapper(var response: HttpResponse) extends Response {

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
}
