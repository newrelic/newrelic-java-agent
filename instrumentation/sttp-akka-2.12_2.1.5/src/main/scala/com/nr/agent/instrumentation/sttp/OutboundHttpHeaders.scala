/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.newrelic.api.agent.{HeaderType, OutboundHeaders}
import sttp.client.Request

class OutboundHttpHeaders[T, S](val request: Request[T, S]) extends OutboundHeaders {
  override def getHeaderType = HeaderType.HTTP

  /**
    * Sets a response header with the given name and value.
    * NO-OP Sttp Request Headers are immutable and so can't be set from here
    */
  override def setHeader(name: String, value: String): Unit = request.header(name, value)
}
