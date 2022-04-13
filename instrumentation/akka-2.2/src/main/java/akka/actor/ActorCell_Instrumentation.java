/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.actor;

import akka.dispatch.Envelope_Instrumentation;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.akka22.AkkaUtil;

@Weave(originalName = "akka.actor.ActorCell")
public abstract class ActorCell_Instrumentation {
    @Trace(async = true)
    public void invoke(Envelope_Instrumentation envelope) {
        String receiver = (actor() == null) ? null : actor().getClass().getName();

        String messageClassName = envelope.message().getClass().getName();
        if (receiver != null && !AkkaUtil.isHeartBeatMessage(messageClassName) && !AkkaUtil.isLogger(receiver)) {
          if (envelope.token != null) {
            envelope.token.link();
            AgentBridge.getAgent().getTracedMethod().setMetricName("Akka", "receive", receiver);
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW,
                                                                       false, "Actor", receiver, "invoke");

            envelope.token.expire();
            envelope.token = null;
          }
        }

        Weaver.callOriginal();
    }

    public abstract Actor actor();
}
