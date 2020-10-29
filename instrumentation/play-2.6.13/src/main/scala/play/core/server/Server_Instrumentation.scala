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
import play.api.mvc.{Handler, RequestHeader, Result}
import play.core.ApplicationProvider

import scala.concurrent._;

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "play.core.server.Server")
class Server_Instrumentation {
  @Trace(dispatcher = true)
  def getHandlerFor(
    request: RequestHeader,
    applicationProvider: ApplicationProvider
  ): Either[Future[Result], (RequestHeader, Handler)] = {
    val tempResult: Either[Future[Result], (RequestHeader, Handler)] = Weaver.callOriginal()
    val result = tempResult.right.map { case (request, handler) =>
      // In order to correctly track async work from here we need to stash a token into the request.
      val token = NewRelic.getAgent.getTransaction.getToken
      (PlayUtil.appendToken(request, token), handler)
    }

    val tracer = NewRelic.getAgent.getTracedMethod
    if (tracer != null) {
      tracer.setMetricName("Play2Routing");
    }

    result
  }

}
