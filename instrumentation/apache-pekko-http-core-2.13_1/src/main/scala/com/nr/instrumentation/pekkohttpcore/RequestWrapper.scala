/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekkohttpcore

import java.util

import org.apache.pekko.http.scaladsl.model.HttpRequest
import com.newrelic.api.agent.{ExtendedRequest, HeaderType}

import scala.jdk.CollectionConverters._

class RequestWrapper(request: HttpRequest) extends ExtendedRequest {

  def getMethod: String = {
    request.method.name
  }

  def getRequestURI: String = {
    request.uri.path.toString()
  }

  def getRemoteUser: String = {
    null
  }

  def getParameterNames: java.util.Enumeration[_] = {
    request.uri.query().toMap.keysIterator.asJavaEnumeration
  }

  def getParameterValues(name: String): Array[String] = {
    request.uri.query().getAll(name).toArray
  }

  def getAttribute(name: String): AnyRef = {
    null
  }

  def getCookieValue(name: String): String = {
    request.cookies.find(cookie => cookie.name.equalsIgnoreCase(name)).map(cookie => cookie.value).orNull
  }

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
    headers.asJava
  }
}
