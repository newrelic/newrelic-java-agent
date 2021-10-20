/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akkahttpcore


import akka.http.scaladsl.model.HttpResponse
import com.newrelic.agent.bridge.{AgentBridge, Token}
import com.newrelic.api.agent.{ Transaction}
import com.newrelic.api.agent.weaver.Weaver

import scala.concurrent.{ExecutionContext, Future}

object ResponseFuture {


}
