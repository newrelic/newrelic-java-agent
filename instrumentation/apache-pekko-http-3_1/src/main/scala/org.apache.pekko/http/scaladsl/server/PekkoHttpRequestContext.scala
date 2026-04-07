/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server

import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.scaladsl.marshalling.PekkoHttpToResponseMarshallable
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import org.apache.pekko.stream.Materializer
import com.agent.instrumentation.org.apache.pekko.http.PathMatcherUtils
import com.newrelic.api.agent.weaver.{Weave, Weaver}

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

@Weave(originalName = "org.apache.pekko.http.scaladsl.server.RequestContextImpl")
abstract class PekkoHttpRequestContext(request: HttpRequest,
                                      unmatchedPath: Uri.Path,
                                      executionContext: ExecutionContextExecutor,
                                      materializer: Materializer,
                                      log: LoggingAdapter,
                                      settings: RoutingSettings,
                                      parserSettings: ParserSettings) {

  def complete(trm: PekkoHttpToResponseMarshallable): Future[RouteResult] = {
    val contextWrapper = PathMatcherUtils.nrRequestContext.get()
    if (trm != null && contextWrapper != null) {
      trm.token = contextWrapper.token
    }
    Weaver.callOriginal() // This ends up calling complete on our NewRelicRequestContextWrapper
  }

  def reconfigure(executionContext: ExecutionContextExecutor, materializer: Materializer, log: LoggingAdapter, settings: RoutingSettings): RequestContext = {
    Weaver.callOriginal()
  }

  private def copy(request: HttpRequest,
                   unmatchedPath: Uri.Path,
                   executionContext: ExecutionContextExecutor,
                   materializer: Materializer,
                   log: LoggingAdapter,
                   settings: RoutingSettings,
                   parserSettings: ParserSettings): RequestContextImpl = {
    return new NewRelicRequestContextWrapper(this, Weaver.callOriginal(), null, new LinkedBlockingDeque[String](),
      new AtomicBoolean(false), new AtomicInteger(0), new AtomicInteger(0), new LinkedBlockingDeque[String], new mutable.HashSet[String], request,
      unmatchedPath, executionContext, materializer, log, settings, parserSettings)
  }
}
