/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import sttp.client.monad.MonadError
import sttp.client.ws.WebSocketResponse
import sttp.client.Request
import sttp.client.{FollowRedirectsBackend, Response, SttpBackend}
import com.nr.agent.instrumentation.sttp.SttpUtils.{finishSegment, startSegment}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


class DelegateFuture(delegate: FollowRedirectsBackend[Future, Any, Any]) extends SttpBackend[Future, Any, Any] {

  implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  override def send[T](request: Request[T, Any]): Future[Response[T]] = {
    val segment = startSegment(request)

    val response = delegate.send(request)

    response onComplete {
      case Success(response) => finishSegment(request, segment, response)
      case Failure(_) => segment.end()
    }

    response
  }

  override def openWebsocket[T, WS_RESULT](request: Request[T, Any], handler: Any): Future[WebSocketResponse[WS_RESULT]] = delegate.openWebsocket(request, handler)

  override def close(): Future[Unit] = delegate.close()

  override def responseMonad: MonadError[Future] = delegate.responseMonad
}
