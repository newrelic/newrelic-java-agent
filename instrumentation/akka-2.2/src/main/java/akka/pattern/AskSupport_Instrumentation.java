/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.pattern;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.akka22.AkkaUtil;

@Weave(type = MatchType.Interface, originalName = "akka.pattern.AskSupport")
public class AskSupport_Instrumentation {

    @Trace
    public ActorRef ask(ActorRef actorRef) {
        String receiver = AkkaUtil.getActor(actorRef);

        if (!AkkaUtil.isLogger(receiver)) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("Akka", "ask", receiver);
        }
        return Weaver.callOriginal();
    }

    @Trace
    public ActorSelection ask(ActorSelection actorSelection) {
        ActorRef actorRef = actorSelection.anchor();
        String receiver = AkkaUtil.getActor(actorRef);

        if (!AkkaUtil.isLogger(receiver)) {
            AgentBridge.getAgent().getTracedMethod().setMetricName("Akka", "ask", receiver);
        }
        return Weaver.callOriginal();
    }

}
