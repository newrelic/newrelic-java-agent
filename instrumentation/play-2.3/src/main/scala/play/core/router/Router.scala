/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.core

import com.newrelic.api.agent.weaver.{MatchType,Weave,Weaver};
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import java.util.logging.Level;

import play.api.mvc.Handler;
import play.core.Router.HandlerDef;
import play.core.Router.HandlerInvokerFactory;
import play.core.Router.HandlerInvoker;

/**
 * Routes is nested in the Router class.
 * However, the weaver thinks we're trying to match non-static classes if we express that in our instrumentation.
 */
@Weave(`type` = MatchType.Interface)
class Router$Routes {
  def createInvoker[T](fakeCall: => T, handlerDef: HandlerDef)(implicit hif: HandlerInvokerFactory[T]): HandlerInvoker[T] = {
    return new NewRelicWrapperInvoker(Weaver.callOriginal(), handlerDef)
  }
}

//Util classes

class NewRelicWrapperInvoker[A](underlyingInvoker: HandlerInvoker[A], handlerDef: HandlerDef) extends HandlerInvoker[A] {
  val PLAY_CONTROLLER_ACTION: String = "PlayControllerAction"
  val txName = handlerDef.controller + "." + handlerDef.method
  def call(call: => A): Handler = {
    try {
      AgentBridge.getAgent.getLogger.log(Level.FINE, "Setting transaction name to \"{0}\" using Play 2.3 controller action", txName);
      AgentBridge.getAgent.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, PLAY_CONTROLLER_ACTION, txName);
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
    }
    return underlyingInvoker.call(call);
  }
}
