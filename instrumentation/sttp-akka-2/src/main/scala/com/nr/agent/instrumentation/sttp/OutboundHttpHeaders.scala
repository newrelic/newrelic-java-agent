/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.newrelic.api.agent.{HeaderType, OutboundHeaders}
import sttp.client.Request
import sttp.model.Header

class OutboundHttpHeaders[T, S](val request: Request[T, S]) extends OutboundHeaders {
  override def getHeaderType = HeaderType.HTTP

  override def setHeader(name: String, value: String): Unit = {
    request.headers :+ Header(name, value)
  }
}
