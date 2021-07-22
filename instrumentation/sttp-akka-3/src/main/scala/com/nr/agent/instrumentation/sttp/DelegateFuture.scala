/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sttp

import com.nr.agent.instrumentation.sttp.SttpAkka3Utils.{finishSegment, startSegment}
import sttp.capabilities.Effect
import sttp.client3.{DelegateSttpBackend, Request, Response, SttpBackend}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class DelegateFuture[WS](delegate: SttpBackend[Future, WS]) extends DelegateSttpBackend[Future, WS] (delegate) {

  implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  override def send[T, R >: WS with Effect[Future]](request: Request[T, R]): Future[Response[T]] = {
    val segment = startSegment(request)

    val response = delegate.send(request)

    response onComplete {
      case Success(response) => finishSegment(request, segment, response)
      case Failure(_) => segment.end()
    }

    response
  }
}
