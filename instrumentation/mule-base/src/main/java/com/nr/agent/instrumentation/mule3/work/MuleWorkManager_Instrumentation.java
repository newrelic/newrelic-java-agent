/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.work;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.api.work.WorkExecutor;
import org.mule.work.WorkerContext;

@Weave(type = MatchType.ExactClass, originalName = "org.mule.work.MuleWorkManager")
public abstract class MuleWorkManager_Instrumentation {

    private final String name = Weaver.callOriginal();

    @Trace
    private void executeWork(WorkerContext work, WorkExecutor workExecutor) {
        final Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            TracedMethod tm = (TracedMethod) txn.getTracedMethod();
            if (tm != null) {
                tm.setMetricName("Flow", "MuleWorkManager", "executeWork", name);
            }
        }

        Weaver.callOriginal();
    }

}
