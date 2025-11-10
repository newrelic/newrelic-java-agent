/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekko1.test.actors.routing;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
//import org.apache.pekko.actor.UntypedActor;
import org.apache.pekko.japi.pf.FI;
import org.apache.pekko.japi.pf.ReceiveBuilder;
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
                NewRelic.setTransactionName("Pekko", "Routing");
                NewRelic.getAgent().getTracedMethod().addRollupMetricName("RoutingActor");
                if (routee != null) {
                    routee.forward(message, getContext());
                }

            }
        }).build();
    }

}
