/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekko1.test.actors.broadcasting;

import org.apache.pekko.actor.AbstractActor;
//import org.apache.pekko.actor.UntypedActor;
import org.apache.pekko.japi.pf.FI;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.newrelic.api.agent.NewRelic;

public class ActorB extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(new FI.UnitApply<Object>() {
            @Override
            public void apply(Object message) throws Exception {
                NewRelic.getAgent().getTracedMethod().addRollupMetricName("ActorB");
            }
        }).build();
    }
}
