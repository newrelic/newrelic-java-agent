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
import com.nr.agent.instrumentation.play26.PlayUtil
import play.api.Application
import play.api.mvc.{Handler, RequestHeader, Result}

import scala.concurrent._;

@ScalaWeave(`type` = ScalaMatchType.Trait, `originalName` = "play.core.server.Server")
class Server_Instrumentation {
  @Trace(dispatcher = true)
  def getHandlerFor(request: RequestHeader): Either[Future[Result], (RequestHeader, Handler, Application)] = {
    var result: Either[Future[Result], (RequestHeader, Handler, Application)] = Weaver.callOriginal()
    if (result.isRight) {
      // In order to correctly track async work from here we need to stash a token into the request.
      val token = NewRelic.getAgent.getTransaction.getToken
      result = PlayUtil.appendToken(result, token)
    }

    val tracer = NewRelic.getAgent.getTracedMethod
    if (tracer != null) {
      tracer.setMetricName("Play2Routing");
    }

    result
  }

}
