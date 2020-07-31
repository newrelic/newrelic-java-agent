/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.deadlock;

import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.errors.DeadlockTraceError;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.service.ServiceFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class DeadLockDetector {
    private static final int MAX_THREAD_DEPTH = 300;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final ErrorCollectorConfig errorCollectorConfig;

    public DeadLockDetector(ErrorCollectorConfig errorCollectorConfig) {
        this.errorCollectorConfig = errorCollectorConfig;
    }

    protected void detectDeadlockedThreads() {
        ThreadInfo[] threadInfos = getDeadlockedThreadInfos();
        if (threadInfos.length > 0) {
            Agent.LOG.info(MessageFormat.format("Detected {0} deadlocked threads", threadInfos.length));
            if (Agent.isDebugEnabled()) {
                boolean harvestThreadLocked = false;
                for (ThreadInfo threadInfo : threadInfos) {
                    if (threadInfo.getThreadName().equals(HarvestServiceImpl.HARVEST_THREAD_NAME)) {
                        harvestThreadLocked = true;
                    }
                }
                if (harvestThreadLocked) {
                    Agent.LOG.severe("A harvest thread deadlock condition was detected");
                    return;
                }
            }
            reportDeadlocks(Arrays.asList(threadInfos));
        }
    }

    ThreadInfo[] getDeadlockedThreadInfos() {
        long[] deadlockedThreadIds = findDeadlockedThreads();
        if (deadlockedThreadIds == null) {
            return new ThreadInfo[0];
        }
        return threadMXBean.getThreadInfo(deadlockedThreadIds, MAX_THREAD_DEPTH);
    }

    protected ThreadMXBean getThreadMXBean() {
        return threadMXBean;
    }

    protected long[] findDeadlockedThreads() {
        try {
            return getThreadMXBean().findDeadlockedThreads();
        } catch (UnsupportedOperationException e) {
            return getThreadMXBean().findMonitorDeadlockedThreads();
        }
    }

    private void reportDeadlocks(List<ThreadInfo> deadThreads) {
        TracedError[] tracedErrors = getTracedErrors(deadThreads);
        getErrorService().reportErrors(tracedErrors);
    }

    private ErrorService getErrorService() {
        return ServiceFactory.getRPMService().getErrorService();
    }

    /**
     * Returns a list of traced errors for the given deadlocked threads. This list should be smaller than the original
     * list of threads - one error is created for each pair of deadlocked threads, and the error contains a stack trace
     * for each thread.
     */
    TracedError[] getTracedErrors(List<ThreadInfo> threadInfos) {
        Map<Long, ThreadInfo> idToThreads = new HashMap<>();
        for (ThreadInfo thread : threadInfos) {
            idToThreads.put(thread.getThreadId(), thread);
        }

        List<TracedError> errors = new ArrayList<>();
        Set<Long> skipIds = new HashSet<>();
        StringBuffer deadlockMsg = new StringBuffer();
        for (ThreadInfo thread : threadInfos) {
            if (!skipIds.contains(thread.getThreadId())) {
                long otherId = thread.getLockOwnerId();
                skipIds.add(otherId);
                ThreadInfo otherThread = idToThreads.get(otherId);
                // four to preserve memory and not cause a new map to be created
                Map<String, String> parameters = Maps.newHashMapWithExpectedSize(4);
                parameters.put(AttributeNames.THREAD_NAME, thread.getThreadName());
                Map<String, StackTraceElement[]> stackTraces = new HashMap<>();
                stackTraces.put(thread.getThreadName(), thread.getStackTrace());
                if (otherThread != null) {
                    parameters.put(AttributeNames.LOCK_THREAD_NAME, otherThread.getThreadName());
                    stackTraces.put(otherThread.getThreadName(), otherThread.getStackTrace());
                }
                if (!errors.isEmpty()) {
                    deadlockMsg.append(", ");
                }
                deadlockMsg.append(thread.toString());
                errors.add(DeadlockTraceError
                        .builder(errorCollectorConfig, null, System.currentTimeMillis())
                        .threadInfoAndStackTrace(thread, stackTraces)
                        .errorAttributes(parameters)
                        .build());
            }
        }

        Agent.LOG.log(Level.FINER, "There are {0} deadlocked thread(s): [{1}]", errors.size(), deadlockMsg);
        return errors.toArray(new TracedError[0]);
    }

}
