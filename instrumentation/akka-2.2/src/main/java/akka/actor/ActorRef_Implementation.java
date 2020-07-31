/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.actor;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.akka22.AkkaUtil;

@Weave(originalName = "akka.actor.ActorRef")
public abstract class ActorRef_Implementation {

    @Trace
    public void forward(Object message, ActorContext context) {
        String name = path().name();

        if (!AkkaUtil.isHeartBeatMessage(message.getClass().getName()) && !AkkaUtil.isLogger(name)) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("Akka", "forward", name);
        }

        Weaver.callOriginal();
    }

    public abstract ActorPath path();
}
