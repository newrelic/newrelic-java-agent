/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.Agent;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;

import java.util.logging.Level;

class AdaptiveSampling {

    static int decidedLast(SamplingPriorityQueue<?> reservoir, int target) {
        if (reservoir == null) {
            return 0;
        }

        if (reservoir.getServiceName().equals("Span Event Service")) {
            Agent.LOG.log(Level.FINER, "*SpanEvent* Application \"{0}\" decided {1} event(s) for {2}, sampled {3} of them with a target of {4}, decided {5} last time",
                    reservoir.getAppName(), reservoir.getDecided(), reservoir.getServiceName(), reservoir.getSampled(), target, reservoir.getDecidedLast());
        }

        return reservoir.getDecided();
    }

}
