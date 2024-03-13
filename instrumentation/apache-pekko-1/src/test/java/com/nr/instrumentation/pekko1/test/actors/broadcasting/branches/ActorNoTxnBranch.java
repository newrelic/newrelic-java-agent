/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekko1.test.actors.broadcasting.branches;

import org.apache.pekko.actor.AbstractActor;
//import org.apache.pekko.actor.UntypedActor;
import org.apache.pekko.japi.pf.FI;
import org.apache.pekko.japi.pf.ReceiveBuilder;
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
