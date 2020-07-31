/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.routing

import java.util
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorRef
import com.agent.instrumentation.spray.PathMatcherUtils
import com.newrelic.agent.bridge.{AgentBridge, Transaction, TransactionNamePriority}
import spray.http.{HttpRequest, Uri}
import spray.httpx.marshalling.ToResponseMarshaller

class NewRelicRequestContextWrapper(originalRequestContext: Object,
                                    underlyingRequestContext: RequestContext,
                                    var matchedPath: util.Deque[String],
                                    var divertRepeat: AtomicBoolean,
                                    var repeatHolder: util.Deque[String],
                                    request: HttpRequest,
                                    responder: ActorRef,
                                    unmatchedPath: Uri.Path)
  extends RequestContext(request, responder, unmatchedPath) {
    PathMatcherUtils.nrRequestContext.set(this)
    originalRequestContext match {
      case wrapper: NewRelicRequestContextWrapper =>
        matchedPath(wrapper.matchedPath)
        divertRepeat(wrapper.divertRepeat)
        repeatHolder(wrapper.repeatHolder)
      case _ =>
    }

  override def complete[T](obj: T)(implicit marshaller: ToResponseMarshaller[T]): Unit = {
    val transaction: Transaction = AgentBridge.getAgent.getTransaction(false)
    if (transaction != null) {
      val transactionName = PathMatcherUtils.finishPathAndGetTransactionName(this)
      transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SprayHttp", transactionName)
    }
    underlyingRequestContext.complete(obj)
  }

  def matchedPath(matchedPath: util.Deque[String]): Unit = {
    this.matchedPath = matchedPath
  }

  def divertRepeat(divertRepeat: AtomicBoolean): Unit = {
    this.divertRepeat = divertRepeat
  }

  def repeatHolder(repeatHolder: util.Deque[String]): Unit = {
    this.repeatHolder = repeatHolder
  }
}