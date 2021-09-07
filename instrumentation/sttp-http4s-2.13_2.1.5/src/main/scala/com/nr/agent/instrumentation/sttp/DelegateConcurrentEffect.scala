/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import cats.effect.{ConcurrentEffect, ContextShift}
import com.nr.agent.instrumentation.sttp.SttpUtils.{finishSegment, startSegment}
import fs2.Stream
import sttp.client.monad.MonadError
import sttp.client.ws.WebSocketResponse
import sttp.client.{FollowRedirectsBackend, NothingT, Request, Response, SttpBackend}

class DelegateConcurrentEffect[F[_]: ConcurrentEffect: ContextShift](delegate: FollowRedirectsBackend[F, Stream[F, Byte], NothingT]) extends SttpBackend[F, Stream[F, Byte], NothingT] {

  override def send[T](request: Request[T, Stream[F, Byte]]): F[Response[T]] = {
    val segment = startSegment(request)

    val handleResponse = delegate.send(request)

    val handleError = ConcurrentEffect[F].handleErrorWith(handleResponse)(f => {
      segment.end()
      ConcurrentEffect[F].raiseError(f)
    })
    val response = ConcurrentEffect[F].map(handleError)(response => {
      finishSegment(request, segment, response)
      response
    })

    response
  }

  override def openWebsocket[T, WS_RESULT](request: Request[T, Stream[F, Byte]], handler: NothingT[WS_RESULT]): F[WebSocketResponse[WS_RESULT]] = delegate.openWebsocket(request, handler)

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}
