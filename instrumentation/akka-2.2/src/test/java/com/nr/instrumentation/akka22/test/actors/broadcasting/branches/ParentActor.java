/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.broadcasting.branches;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
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
                NewRelic.setTransactionName("Akka", "ParentActor");
                childActor.forward(message, getContext());
                Thread.sleep(2000);
            }
        }).build();
    }
}
