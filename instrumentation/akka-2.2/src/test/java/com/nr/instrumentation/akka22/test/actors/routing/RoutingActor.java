/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.routing;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import com.newrelic.api.agent.NewRelic;

public class RoutingActor extends AbstractActor {

    private final ActorRef routee;

    public RoutingActor() {
        this.routee = null;
    }

    public RoutingActor(ActorRef routee) {
        this.routee = routee;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(new FI.UnitApply<Object>() {
            @Override
            public void apply(Object message) {
                NewRelic.setTransactionName("Akka", "Routing");
                NewRelic.getAgent().getTracedMethod().addRollupMetricName("RoutingActor");
                if (routee != null) {
                    routee.forward(message, getContext());
                }

            }
        }).build();
    }

}
