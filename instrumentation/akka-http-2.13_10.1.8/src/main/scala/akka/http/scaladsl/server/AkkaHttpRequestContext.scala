/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.AkkaHttpToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import com.agent.instrumentation.akka.http.PathMatcherUtils
import com.newrelic.api.agent.weaver.{Weave, Weaver}

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

@Weave(originalName = "akka.http.scaladsl.server.RequestContextImpl")
abstract class AkkaHttpRequestContext(request: HttpRequest,
                                      unmatchedPath: Uri.Path,
                                      executionContext: ExecutionContextExecutor,
                                      materializer: Materializer,
                                      log: LoggingAdapter,
                                      settings: RoutingSettings,
                                      parserSettings: ParserSettings) {

  def complete(trm: AkkaHttpToResponseMarshallable): Future[RouteResult] = {
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
