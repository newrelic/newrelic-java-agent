/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.broadcasting.branches;

import akka.actor.AbstractActor;
import akka.actor.UntypedActor;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import com.newrelic.api.agent.NewRelic;

public class ActorNoTxnBranch extends AbstractActor {
    public static final String  ROLLUP_NAME = "ActorNoTxnBranch";

    private static int count = 0;

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(new FI.UnitApply<Object>() {
            @Override
            public void apply(Object message) throws Exception {
                if (count == 1000) {
                    return;
                }
                count++;

                NewRelic.getAgent().getTracedMethod().addRollupMetricName(ROLLUP_NAME);
                self().forward(message, getContext());
            }
        }).build();
    }
}
