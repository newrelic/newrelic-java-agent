/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.core.server

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}
import play.api.Application
import play.api.mvc.{Handler, RequestHeader, Result}

import scala.concurrent.Future


@Weave(`type` = MatchType.Interface)
class Server {
  @Trace(dispatcher = true)
  def getHandlerFor(request: RequestHeader): Either[Future[Result], (RequestHeader, Handler, Application)] = {
    val result: Either[Future[Result], (RequestHeader, Handler, Application)] = Weaver.callOriginal()
    val tracer = AgentBridge.getAgent.getTransaction.getTracedMethod
    if (tracer != null) {
      tracer.setMetricName("Play2Routing")
    }

    result
  }
}
