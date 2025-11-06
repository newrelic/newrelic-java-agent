/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import sttp.model.Header

import scala.jdk.CollectionConverters._
import java.util

class InboundHttpHeaders[T](headers: Seq[Header]) extends ExtendedInboundHeaders {
  override def getHeader(name: String): String = {
    val list: util.List[Header] = headers.filter(x => x.name == name).toList.asJava
    if (list == null || list.isEmpty) null else list.get(0).value
  }

  override def getHeaders(name: String): util.List[String] =
    headers
      .filter(x => x.name == name)
      .map(x => x.value)
      .toList.asJava

  override def getHeaderType: HeaderType = HeaderType.HTTP
}
