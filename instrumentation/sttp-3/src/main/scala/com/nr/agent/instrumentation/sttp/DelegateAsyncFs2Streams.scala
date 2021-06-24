/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import cats.effect.Async
import com.nr.agent.instrumentation.sttp.SttpUtils.{finishSegment, startSegment}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.{DelegateSttpBackend, Request, Response, SttpBackend}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class DelegateAsyncFs2Streams[F[_]: Async](delegate: SttpBackend[F, Fs2Streams[F]]) extends DelegateSttpBackend[F, Fs2Streams[F]] (delegate) {

  implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  type PE = Fs2Streams[F] with sttp.capabilities.Effect[F]

  override def send[T, R >: PE](request: Request[T, R]): F[Response[T]] = {
    val segment = startSegment(request)

    val handleResponse: F[Response[T]] = delegate.send(request)

    val handleError = Async[F].handleErrorWith(handleResponse)(f => {
      segment.end()
      Async[F].raiseError(f)
    })
    val response: F[Response[T]] = Async[F].map(handleError)(response => {
      finishSegment(request, segment, response)
      response
    })

    response
  }
}
