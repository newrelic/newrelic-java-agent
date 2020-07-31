/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.routing;

import akka.actor.AbstractActor;
import akka.actor.UntypedActor;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import com.newrelic.api.agent.NewRelic;

public class Routee extends AbstractActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(new FI.UnitApply<Object>() {
            @Override
            public void apply(Object messsage) {
                NewRelic.setTransactionName("Akka", "Routee");
                NewRelic.getAgent().getTracedMethod().addRollupMetricName("Routee");

            }
        }).build();
    }
}
