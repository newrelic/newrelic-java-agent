/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.AccessControlException;
import java.text.MessageFormat;

public class ThreadSampler implements MetricSampler {

    private final ThreadMXBean threadMXBean;
    private final boolean isDeadlockDetectorEnabled;

    public ThreadSampler() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        isDeadlockDetectorEnabled = config.getValue("deadlock_detector.enabled", true);
    }

    @Override
    public void sample(StatsEngine statsEngine) {
        int threadCount = threadMXBean.getThreadCount();
        statsEngine.getStats(MetricNames.THREAD_COUNT).setCallCount(threadCount);

        if (isDeadlockDetectorEnabled) {
            long[] deadlockedThreadIds;
            try {
                deadlockedThreadIds = threadMXBean.findMonitorDeadlockedThreads();
            } catch (AccessControlException e) {
                Agent.LOG.warning(MessageFormat.format("An error occurred calling ThreadMXBean.findMonitorDeadlockedThreads: {0}", e));
                deadlockedThreadIds = new long[0];
            }
            int deadlockCount = deadlockedThreadIds == null ? 0 : deadlockedThreadIds.length;
            statsEngine.getStats(MetricNames.THREAD_DEADLOCK_COUNT).setCallCount(deadlockCount);
        }
    }

}
