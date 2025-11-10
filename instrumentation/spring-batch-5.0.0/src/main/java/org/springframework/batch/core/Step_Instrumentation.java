/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.batch.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "org.springframework.batch.core.Step")
public class Step_Instrumentation {
    @Trace
    public void execute(StepExecution stepExecution) throws JobInterruptedException {
        Weaver.callOriginal();
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("SpringBatch", "Step", "execute", stepExecution.getStepName());
        }
    }
}
