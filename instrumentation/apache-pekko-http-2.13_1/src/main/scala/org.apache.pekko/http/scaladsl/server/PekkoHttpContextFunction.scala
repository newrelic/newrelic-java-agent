/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server

import com.agent.instrumentation.org.apache.pekko.http.PathMatcherUtils
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.Trace

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.logging.Level
import scala.collection.mutable
import scala.concurrent.Future
import scala.runtime.AbstractFunction1

object PekkoHttpContextFunction {

  final val retransformed = new AtomicBoolean(false)

  def contextWrapper(original: Function1[RequestContext, Future[RouteResult]]): Function1[RequestContext, Future[RouteResult]] = {
    if (retransformed.compareAndSet(false, true)) {
      AgentBridge.getAgent.getLogger.log(Level.FINER, "Retransforming org.apache.pekko.http.scaladsl.server.PekkoHttpContextFunction")
      AgentBridge.instrumentation.retransformUninstrumentedClass(classOf[ContextWrapper])
      AgentBridge.getAgent.getLogger.log(Level.FINER, "Retransformed org.apache.pekko.http.scaladsl.server.PekkoHttpContextFunction")
    }

    new ContextWrapper(original)
  }

}

// REVISIT THIS AND UNPACK IT

class ContextWrapper(original: Function1[RequestContext, Future[RouteResult]]) extends AbstractFunction1[RequestContext, Future[RouteResult]] {

  @Trace(dispatcher = true)
  override def apply(ctx: RequestContext): Future[RouteResult] = {
    try {
      val tracedMethod = AgentBridge.getAgent.getTracedMethod
      tracedMethod.setMetricName("PekkoHttp")
      // Pekko-http 10.1.5 uses CallbackRunnable and we lose transaction context between Directives
      AgentBridge.getAgent.getTracedMethod.setTrackCallbackRunnable(true);
      val token = AgentBridge.getAgent.getTransaction(false).getToken
      PathMatcherUtils.setHttpRequest(ctx.request)
      // We use this method to wire up our RequestContext wrapper and start our transaction
      val newCtx = new NewRelicRequestContextWrapper(ctx, ctx.asInstanceOf[RequestContextImpl], token,
        new LinkedBlockingDeque[String], new AtomicBoolean(false), new AtomicInteger(0), new AtomicInteger(0),
        new LinkedBlockingDeque[String], new mutable.HashSet[String], ctx.request, ctx.unmatchedPath, ctx.executionContext, ctx.materializer,
        ctx.log, ctx.settings, ctx.parserSettings)
      original.apply(newCtx)
    } catch {
      case t: Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, "pekko-http-2.4.5")
        original.apply(ctx)
      }
    }
  }

  override def compose[A](g: (A) => RequestContext): (A) => Future[RouteResult] = original.compose(g)

  override def andThen[A](g: (Future[RouteResult]) => A): (RequestContext) => A = original.andThen(g)

  override def toString(): String = original.toString()

}
