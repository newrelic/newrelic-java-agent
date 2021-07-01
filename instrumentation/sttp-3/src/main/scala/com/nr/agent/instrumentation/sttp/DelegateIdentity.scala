/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.nr.agent.instrumentation.sttp.Sttp3Utils.{finishSegment, startSegment}
import sttp.capabilities.Effect
import sttp.client3.{DelegateSttpBackend, Identity, Request, Response, SttpBackend}

class DelegateIdentity(delegate: SttpBackend[Identity, Any]) extends DelegateSttpBackend[Identity, Any](delegate) {
  override def send[T, R >: Any with Effect[Identity] ](request: Request[T, R]): Response[T] = {
    val segment = startSegment(request)

    val response = delegate.send(request)

    finishSegment(request, segment, response)

    response
  }
}
