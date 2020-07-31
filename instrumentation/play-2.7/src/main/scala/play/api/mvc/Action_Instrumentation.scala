/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.mvc

import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.nr.agent.instrumentation.play27.PlayUtil

import scala.concurrent.Future

@ScalaWeave(`type` = ScalaMatchType.Trait, `originalName` = "play.api.mvc.Action")
class Action_Instrumentation[A] {
  @Trace(async = true, metricName = "Play2Controller")
  def apply(request: Request[A]): Future[Result] = {
    val tokenOption = request.attrs.get(PlayUtil.newRelicToken)
    if (tokenOption.isDefined) {
      tokenOption.get.linkAndExpire()
    }

    Weaver.callOriginal()
  }
}
