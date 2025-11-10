/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.server

import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import com.agent.instrumentation.akka.http.PathMatcherUtils
import com.newrelic.agent.bridge.{AgentBridge, Token}
import com.newrelic.api.agent.{Trace, TransactionNamePriority}

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class NewRelicRequestContextWrapper(originalRequestContext: Object,
                                    underlyingRequestContext: RequestContextImpl,
                                    var token: Token,
                                    var matchedPath: java.util.Deque[String],
                                    var divertRepeat: AtomicBoolean,
                                    var currentMatchedQueueLength: AtomicInteger,
                                    var previousMatchedQueueLength: AtomicInteger,
                                    var repeatHolder: java.util.Deque[String],
                                    var regexHolder: mutable.Set[String],
                                    request: HttpRequest,
                                    unmatchedPath: Uri.Path,
                                    executionContext: ExecutionContextExecutor,
                                    materializer: Materializer,
                                    log: LoggingAdapter,
                                    settings: RoutingSettings,
                                    parserSettings: ParserSettings)
  extends RequestContextImpl(request, unmatchedPath, executionContext, materializer, log, settings, parserSettings) {
  PathMatcherUtils.nrRequestContext.set(this)
  originalRequestContext match {
    case wrapper: NewRelicRequestContextWrapper =>
      token(wrapper.token)
      matchedPath(wrapper.matchedPath)
      divertRepeat(wrapper.divertRepeat)
      currentMatchedQueueLength(wrapper.currentMatchedQueueLength)
      previousMatchedQueueLength(wrapper.previousMatchedQueueLength)
      repeatHolder(wrapper.repeatHolder)
      regexHolder(wrapper.regexHolder)
    case _ =>
  }

  override def reconfigure(executionContext: ExecutionContextExecutor, materializer: Materializer, log: LoggingAdapter,
                           settings: RoutingSettings): RequestContext = {
    underlyingRequestContext.reconfigure(executionContext, materializer, log, settings)
  }

  @Trace(async = true)
  override def complete(trm: ToResponseMarshallable): Future[RouteResult] = {
    try {
      if (token != null) {
        val transactionName = PathMatcherUtils.finishPathAndGetTransactionName(this)
        token.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "AkkaHttp", transactionName)
        token.link()
      }

      underlyingRequestContext.complete(trm).map(result => {
        completeResponse(token)
        result
      })(executionContext)
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, "akka-http-2.4.5")
        underlyingRequestContext.complete(trm)
    }
  }

  @Trace(async = true)
  def completeResponse(token: Token): Unit = {
    try {
      if (token != null) {
        token.linkAndExpire()
      }
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, "akka-http-2.4.5")
    }
  }

  def token(token: Token): Unit = {
    this.token = token
  }

  def matchedPath(matchedPath: java.util.Deque[String]): Unit = {
    this.matchedPath = matchedPath
  }

  def divertRepeat(divertRepeat: AtomicBoolean): Unit = {
    this.divertRepeat = divertRepeat
  }

  def currentMatchedQueueLength(currentMatchedQueueLength: AtomicInteger): Unit = {
    this.currentMatchedQueueLength = currentMatchedQueueLength
  }

  def previousMatchedQueueLength(previousMatchedQueueLength: AtomicInteger): Unit = {
    this.previousMatchedQueueLength = previousMatchedQueueLength
  }

  def repeatHolder(repeatHolder: java.util.Deque[String]): Unit = {
    this.repeatHolder = repeatHolder
  }

  def regexHolder(regexHolder: mutable.Set[String]): Unit = {
    this.regexHolder = regexHolder
  }
}
