/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.github.dwhjames.awswrap;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
  * Anonymous inner runnable which runs the aws command on another thread.
  */
@Weave(originalName="com.github.dwhjames.awswrap.s3.AmazonS3ScalaClient$$anon$1")
public final class S3WrapRunnable implements java.lang.Runnable {
    // need to use java to access this field.
    public final Object request$1 = Weaver.callOriginal();

    @Trace(async = true)
    public void run() {
        if (AgentBridge.getAgent().startAsyncActivity(request$1)) {
            AgentBridge.getAgent().getTransaction().getTracedMethod().setMetricName("S3Wrap");
        }
        Weaver.callOriginal();
    }
}
