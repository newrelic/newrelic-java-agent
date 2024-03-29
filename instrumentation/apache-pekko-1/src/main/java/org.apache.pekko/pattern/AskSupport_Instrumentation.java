/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.pattern;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.pekko1.PekkoUtil;

@Weave(type = MatchType.Interface, originalName = "org.apache.pekko.pattern.AskSupport")
public class AskSupport_Instrumentation {

    @Trace
    public ActorRef ask(ActorRef actorRef) {
        String receiver = PekkoUtil.getActor(actorRef);

        if (!PekkoUtil.isLogger(receiver)) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("Pekko", "ask", receiver);
        }
        return Weaver.callOriginal();
    }

    @Trace
    public ActorSelection ask(ActorSelection actorSelection) {
        ActorRef actorRef = actorSelection.anchor();
        String receiver = PekkoUtil.getActor(actorRef);

        if (!PekkoUtil.isLogger(receiver)) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("Pekko", "ask", receiver);
        }
        return Weaver.callOriginal();
    }

}
