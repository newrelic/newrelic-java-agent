/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.actor;

import akka.dispatch.Envelope_Instrumentation;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.akka22.AkkaUtil;

@Weave(type = MatchType.Interface, originalName = "akka.actor.Cell")
public abstract class Cell_Instrumentation {

    public abstract ActorRef self();

    @Trace
    public void sendMessage(Envelope_Instrumentation envelope) {
        String receiver = props().actorClass().getName();
        String messageClassName = envelope.message().getClass().getName();

        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null && transaction.isStarted() && !AkkaUtil.isHeartBeatMessage(messageClassName) && !AkkaUtil.isLogger(receiver) && !AkkaUtil.isAkkaStream(messageClassName)) {

            String sender = AkkaUtil.getActor(envelope.sender());
            AkkaUtil.recordTellMetric(receiver, sender, messageClassName);

            if (envelope.token != null ) {
                // Akka may migrate envelopes to another ActorCell.
                // See UnstartedCell in RepointableActorRef.scala.
                // We expire and replace the existing token just to be on the safe side.
                envelope.token.expire();
                envelope.token = null; // this prevents the warning even though it's immediately reassigned
            }
            envelope.token = transaction.getToken();
        }

        Weaver.callOriginal();
    }

    public abstract Props props();
}
