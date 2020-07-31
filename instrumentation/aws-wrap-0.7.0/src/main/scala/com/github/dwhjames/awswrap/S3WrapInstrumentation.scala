/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.github.dwhjames.awswrap.s3.instrumentation

import java.io.File
import java.net.URL

import scala.concurrent.Future

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.{Weave, Weaver}

@Weave(originalName="com.github.dwhjames.awswrap.s3.AmazonS3ScalaClient")
class AmazonS3ScalaClient {
  /**
    * In the original code this method is annotated with @inline.
    * In the bytecode it is not actually inlined, so it's safe to instrument.
    * Future versions could fail because scalac decides to actually inline this method.
    */
  private def wrapMethod[Request, Result](
    f:       Request => Result,
    request: Request
  ): Future[Result] = {
    val tx = AgentBridge.getAgent().getTransaction(false)
    if (null != tx && tx.isStarted()) {
      tx.registerAsyncActivity(request);
    }
    Weaver.callOriginal()
  }
}
