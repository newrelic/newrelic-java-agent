/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import cats.effect.Async
import com.nr.agent.instrumentation.sttp.SttpHttp4s3Utils.{finishSegment, startSegment}
import sttp.capabilities.Effect
import sttp.client3.{DelegateSttpBackend, Request, Response, SttpBackend}

class DelegateAsync[F[_]: Async, WS](delegate: SttpBackend[F, WS]) extends DelegateSttpBackend[F, WS] (delegate) {

  override def send[T, R >: WS with Effect[F]](request: Request[T, R]): F[Response[T]] = {
    val segment = startSegment(request)

    val handleResponse = delegate.send(request)

    val handleError = Async[F].handleErrorWith(handleResponse)(f => {
      segment.end()
      Async[F].raiseError(f)
    })
    val response = Async[F].map(handleError)(response => {
      finishSegment(request, segment, response)
      response
    })

    response
  }
}
