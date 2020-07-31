/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.core.routing

import java.util.logging.Level

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.TransactionNamePriority
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}
import play.api.mvc.Handler;

@Weave(`type` = MatchType.Interface)
class HandlerInvokerFactory[T] {
  def createInvoker(fakeCall: => T, handlerDef: HandlerDef): HandlerInvoker[T] = {
    return new NewRelicWrapperInvoker(Weaver.callOriginal(), handlerDef)
  }
}

//Util classes

class NewRelicWrapperInvoker[A](underlyingInvoker: HandlerInvoker[A], handlerDef: HandlerDef) extends HandlerInvoker[A] {
  val PLAY_CONTROLLER_ACTION: String = "PlayControllerAction"
  val txName = handlerDef.controller + "." + handlerDef.method
  def call(call: => A): Handler = {
    try {
      AgentBridge.getAgent.getLogger.log(Level.FINE, "Setting transaction name to \"{0}\" using Play 2.5 controller action", txName);
      AgentBridge.getAgent.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, PLAY_CONTROLLER_ACTION, txName);
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
    }
    return underlyingInvoker.call(call);
  }
}
