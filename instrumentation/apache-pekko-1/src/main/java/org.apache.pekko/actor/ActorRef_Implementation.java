/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.actor;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.pekko1.PekkoUtil;

@Weave(originalName = "org.apache.pekko.actor.ActorRef")
public abstract class ActorRef_Implementation {

    @Trace
    public void forward(Object message, ActorContext context) {
        String name = path().name();

        if (!PekkoUtil.isHeartBeatMessage(message.getClass().getName()) && !PekkoUtil.isLogger(name)) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("Pekko", "forward", name);
        }

        Weaver.callOriginal();
    }

    public abstract ActorPath path();
}
