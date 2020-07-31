/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.routing

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorRef
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}
import spray.http.{HttpRequest, Uri}
import spray.httpx.marshalling.ToResponseMarshaller

@Weave(`type` = MatchType.ExactClass, originalName = "spray.routing.RequestContext")
class SprayRequestContext(request: HttpRequest, responder: ActorRef, unmatchedPath: Uri.Path) {

  @Trace
  def complete[T](obj: T)(implicit marshaller: ToResponseMarshaller[T]): Unit = {
    Weaver.callOriginal() // This ends up calling complete on our NewRelicRequestContextWrapper
  }

  // This allows us to keep our wrapper around the RequestContext
  def copy(request: HttpRequest, responder: ActorRef, unmatchedPath: Uri.Path): RequestContext = {
    new NewRelicRequestContextWrapper(this, Weaver.callOriginal(), new LinkedBlockingDeque[String](),
      new AtomicBoolean(false), new LinkedBlockingDeque[String], request, responder, unmatchedPath)
  }

}