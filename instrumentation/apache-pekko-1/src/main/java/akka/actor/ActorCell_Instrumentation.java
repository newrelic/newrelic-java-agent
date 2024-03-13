/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.actor;

import org.apache.pekko.dispatch.Envelope_Instrumentation;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.pekko1.PekkoUtil;

@Weave(originalName = "org.apache.pekko.actor.ActorCell")
public abstract class ActorCell_Instrumentation {
    @Trace(async = true)
    public void invoke(Envelope_Instrumentation envelope) {
        String receiver = (actor() == null) ? null : actor().getClass().getName();

        String messageClassName = envelope.message().getClass().getName();
        if (receiver != null && !PekkoUtil.isHeartBeatMessage(messageClassName) && !PekkoUtil.isLogger(receiver)) {
            if (envelope.token != null) {
                if (envelope.token.link()) {
                    AgentBridge.getAgent().getTracedMethod().setMetricName("Pekko", "receive", receiver);
                    AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW,
                            false, "Actor", receiver, "invoke");
                }
                envelope.token.expire();
                envelope.token = null;
            }
        }

        Weaver.callOriginal();
    }

    public abstract Actor actor();
}
