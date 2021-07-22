/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import sttp.client.Response
import scala.jdk.CollectionConverters._

import java.util

class InboundHttpHeaders[T](response: Response[T]) extends ExtendedInboundHeaders {
  override def getHeader(name: String): String = response.header(name).orNull

  override def getHeaders(name: String): util.List[String] = response.headers.map(x => x.name).toList.asJava

  override def getHeaderType: HeaderType = HeaderType.HTTP
}
