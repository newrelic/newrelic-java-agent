/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.newrelic.agent.Agent;
import com.newrelic.agent.profile.RunnableThreadRules;
import com.newrelic.agent.profile.ThreadType;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.StackTraces;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sample stack traces for the profiles in a profiling session.
 * 
 * This class is thread-safe but the profiles are not thread-safe.
 */
public class ProfileSampler {

    public static final int MAX_STACK_DEPTH = 300;

    private static final ThreadInfo[] EMPTY_THREAD_INFO_ARRAY = new ThreadInfo[0];
    
    private final RunnableThreadRules runnableThreadRules = new RunnableThreadRules();

    public ProfileSampler() {
    }

    public void sampleStackTraces(List<IProfile> profiles) {
        if (profiles.isEmpty()) {
            return;
        }
        for (IProfile profile : profiles) {
            profile.beforeSampling();
            addThreadInfos(profile, getAllThreadInfos());
        }
    }

    private void addThreadInfos(IProfile profiler, ThreadInfo[] threadInfos) {
        if (threadInfos.length == 0) {
            return;
        }
        Set<Long> agentThreadIds = ServiceFactory.getThreadService().getAgentThreadIds();
        
        for (ThreadInfo threadInfo : threadInfos) {
            if (null != threadInfo) {
                boolean isRunnable = runnableThreadRules.isRunnable(threadInfo);
                if (isRunnable || !profiler.getProfilerParameters().isRunnablesOnly()) {
                    ThreadType type;
                    long threadId = threadInfo.getThreadId();
                    if (agentThreadIds.contains(threadId)) {
                        type = ThreadType.BasicThreadType.AGENT;
                    } else if (profiler.getProfilerParameters().isProfileAgentThreads()
                            && StackTraces.isInAgentInstrumentation(threadInfo.getStackTrace())) {
                        type = ThreadType.BasicThreadType.AGENT_INSTRUMENTATION;
                    } else {
                        type = ThreadType.BasicThreadType.OTHER;
                    }
                    profiler.addStackTrace(threadInfo, isRunnable, type);
                }
            }
        }
    }

    private ThreadInfo[] getAllThreadInfos() {
        long[] threadIds = getAllThreadIds();
        if (threadIds == null || threadIds.length == 0) {
            return EMPTY_THREAD_INFO_ARRAY;
        }
        Set<Long> ids = new HashSet<>(threadIds.length);
        for (long threadId : threadIds) {
            ids.add(threadId);
        }
        ids.remove(Thread.currentThread().getId());
        threadIds = convertToLongArray(ids);
        return getThreadInfos(threadIds);
    }

    private long[] convertToLongArray(Set<Long> ids) {
        long[] arr = new long[ids.size()];
        int i = 0;
        for (Long id : ids) {
            arr[i++] = id;
        }
        return arr;
    }

    private ThreadInfo[] getThreadInfos(long[] threadIds) {
        try {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            if (threadIds.length > 0) {
                return threadMXBean.getThreadInfo(threadIds, MAX_STACK_DEPTH);
            }
        } catch (SecurityException e) {
            Agent.LOG.finer(MessageFormat.format("An error occurred getting thread info: {0}", e));
        }
        return EMPTY_THREAD_INFO_ARRAY;
    }

    private long[] getAllThreadIds() {
        try {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            return threadMXBean.getAllThreadIds();
        } catch (SecurityException e) {
            Agent.LOG.finer(MessageFormat.format("An error occurred getting all thread ids: {0}", e));
            return null;
        }
    }

}
