/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekko1.test.actors.broadcasting.branches;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.japi.pf.FI;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

import static org.junit.Assert.assertNotNull;

public class ParentActor extends AbstractActor {
    private ActorRef childActor = getContext().actorOf(Props.create(ChildActor.class), "child");

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(new FI.UnitApply<Object>() {
            @Override
            @Trace(dispatcher = true)
            public void apply(Object message) throws InterruptedException {
                assertNotNull(AgentBridge.getAgent().getTransaction(false));
                NewRelic.setTransactionName("Pekko", "ParentActor");
                childActor.forward(message, getContext());
                Thread.sleep(2000);
            }
        }).build();
    }
}
