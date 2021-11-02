/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future


object FlowRequestHandler {


  def instrumentFlow(handlerFlow: Flow[HttpRequest, HttpResponse, Any], mat: Materializer)
  : Flow[HttpRequest, HttpResponse, Any] =
    Flow[HttpRequest].mapAsync(1)(new AsyncRequestHandler(toAsyncFunc(handlerFlow)(mat))(mat.executionContext))

  def toAsyncFunc[I, O](flow: Flow[I, O, _])(implicit mat: Materializer) : I => Future[O] =
    i => Source.single(i).via(flow).runWith(Sink.head)

}
