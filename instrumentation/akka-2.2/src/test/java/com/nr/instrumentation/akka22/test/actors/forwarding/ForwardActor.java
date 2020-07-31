/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.forwarding;

import akka.actor.AbstractActor;
import akka.actor.UntypedActor;

import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

public class ForwardActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(new FI.UnitApply<Object>() {
            @Override
            public void apply(Object message) {
                NewRelic.setTransactionName("AkkaForward", "Forward");
            }
        }).build();
    }
}
