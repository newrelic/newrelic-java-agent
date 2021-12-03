/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws

import com.newrelic.api.agent.{HeaderType, OutboundHeaders}

import scala.collection.mutable

class OutboundWrapper(var nrHeaders: mutable.Buffer[(String, String)]) extends OutboundHeaders {

  override def getHeaderType: HeaderType = HeaderType.HTTP

  override def setHeader(name: String, value: String): Unit = {
    nrHeaders += (name -> value)
  }

}
