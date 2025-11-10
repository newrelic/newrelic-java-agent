/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.batch.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "org.springframework.batch.core.Job")
public class Job_Instrumentation {
    @Trace(dispatcher = true)
    public void execute(JobExecution jobExecution) {
        Weaver.callOriginal();

        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            String jobName = jobExecution.getJobInstance().getJobName();
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringBatch", "Job", jobName);

            String status = jobExecution.getExitStatus().getExitCode();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                NewRelic.incrementCounter("SpringBatch/Job/" + jobName + "/" + status);
            }

            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                String metricName = "SpringBatch/Job/" + jobName + "/Step/" + stepExecution.getStepName();
                NewRelic.incrementCounter(metricName + "/read", (int)stepExecution.getReadCount());
                NewRelic.incrementCounter(metricName + "/write", (int)stepExecution.getWriteCount());
                NewRelic.incrementCounter(metricName + "/skip", (int)stepExecution.getSkipCount());
            }
        }
    }
}
