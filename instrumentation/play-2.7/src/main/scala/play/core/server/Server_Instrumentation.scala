/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.core.server

import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.newrelic.api.agent.{NewRelic, Trace}
import com.nr.agent.instrumentation.play27.PlayUtil
import play.api.Application
import play.api.mvc.{Handler, RequestHeader}

import scala.util.Try;

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "play.core.server.Server")
class Server_Instrumentation {

  @Trace(dispatcher = true)
  private[server] def getHandlerFor(request: RequestHeader, tryApp: Try[Application]): (RequestHeader, Handler) = {
    var result: (RequestHeader, Handler) = Weaver.callOriginal()

    // In order to correctly track async work from here we need to stash a token into the request.
    val token = NewRelic.getAgent.getTransaction.getToken
    result = PlayUtil.appendToken(result, token)

    val tracer = NewRelic.getAgent.getTracedMethod
    if (tracer != null) {
      tracer.setMetricName("Play2Routing");
    }

    result
  }

}
