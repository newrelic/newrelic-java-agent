/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.core.routing

import java.util.logging.Level

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.newrelic.api.agent.{NewRelic, TransactionNamePriority}
import play.api.mvc.Handler
import play.api.routing.HandlerDef

@ScalaWeave(`type` = ScalaMatchType.Trait, `originalName` = "play.core.routing.HandlerInvokerFactory")
class HandlerInvokerFactory_Instrumentation[T] {
  def createInvoker(fakeCall: => T, handlerDef: HandlerDef): HandlerInvoker[T] = {
    new NewRelicWrapperInvoker(Weaver.callOriginal(), handlerDef)
  }
}

//Util classes

class NewRelicWrapperInvoker[A](underlyingInvoker: HandlerInvoker[A], handlerDef: HandlerDef) extends HandlerInvoker[A] {
  val PLAY_CONTROLLER_ACTION: String = "PlayControllerAction"
  val txName = handlerDef.controller + "." + handlerDef.method

  def call(call: => A): Handler = {
    try {
      NewRelic.getAgent.getLogger.log(Level.FINE, "Setting transaction name to \"{0}\" using Play 2.6 controller action", txName);
      NewRelic.getAgent.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, PLAY_CONTROLLER_ACTION, txName);
    } catch {
      case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
    }
    underlyingInvoker.call(call);
  }
}
