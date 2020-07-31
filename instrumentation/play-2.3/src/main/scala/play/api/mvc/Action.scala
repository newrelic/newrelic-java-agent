/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.mvc

import com.newrelic.api.agent.weaver.{MatchType,Weave,Weaver};
import com.newrelic.api.agent.Trace;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import java.util.logging.Level;
import scala.concurrent._;

import play.api.mvc.Request;

@Weave(`type` = MatchType.Interface)
class Action[A] {
  @Trace(metricName = "Play2Controller")
  def apply(request: Request[A]): Future[Result] = {
    return Weaver.callOriginal();
  }
}
