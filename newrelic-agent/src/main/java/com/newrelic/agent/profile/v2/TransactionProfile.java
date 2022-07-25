/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Profile information for a specific transaction.
 */
class TransactionProfile implements JSONStreamAware {

    private final ThreadMXBean threadMXBean;
    /**
     * Thread name to profile tree.
     */
    private final LoadingCache<String, ProfileTree> threadProfiles;
    /**
     * Thread name to transaction activity tree (tracers).
     */
    private final LoadingCache<String, TransactionActivityTree> threadActivityProfiles;
    private final ThreadNameNormalizer threadNameNormalizer;        

    public TransactionProfile(final Profile profile, final ThreadNameNormalizer threadNameNormalizer) {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.threadNameNormalizer = threadNameNormalizer;
        threadProfiles = Caffeine.newBuilder().executor(Runnable::run).build(profile.createCacheLoader(false));
        threadActivityProfiles = Caffeine.newBuilder().executor(Runnable::run).build(
                threadName -> new TransactionActivityTree(profile));
    }

    public void addStackTrace(List<StackTraceElement> stackTraceList) {
        String threadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(Thread.currentThread()));
        threadProfiles.getIfPresent(threadName).addStackTrace(stackTraceList, true);
    }

    public void transactionFinished(TransactionData transactionData) {
        
        final List<MetricNameTime> cpuTimes = new ArrayList<>();
        for (TransactionActivity activity : transactionData.getTransactionActivities()) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(activity.getThreadId(), 0);
            if (null != threadInfo) {
                final List<List<StackTraceElement>> backtraces = new ArrayList<>();
                Map<Tracer, Collection<Tracer>> tracerTree = buildChildren(activity.getTracers(), backtraces);
                
                String threadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo));
                threadActivityProfiles.get(threadName).add(activity, tracerTree);
                
                if (!backtraces.isEmpty()) {
                    ProfileTree tree = threadProfiles.get(threadName);
                    
                    for (List<StackTraceElement> stack : backtraces) {
                        stack = DiscoveryProfile.getScrubbedStackTrace(stack);
                        Collections.reverse(stack);
                        
                        tree.addStackTrace(stack, true);
                    }
                }
                
                long cpuTime = activity.getTotalCpuTime();
                if (cpuTime > 0) {
                    MetricName name = MetricName.create(
                            transactionData.getBlameMetricName(), threadName);
                    cpuTimes.add(new MetricNameTime(name, cpuTime));
                }
            }
        }
        
        if (!cpuTimes.isEmpty()) {
            ServiceFactory.getStatsService().doStatsWork(new StatsWork() {
    
                @Override
                public void doWork(StatsEngine statsEngine) {
                    for (MetricNameTime time : cpuTimes) {
                        statsEngine.getResponseTimeStats(time.name).recordResponseTime(time.cpuTime, TimeUnit.NANOSECONDS);
                    }
                }
    
                @Override
                public String getAppName() {
                    return null;
                }
                
            }, transactionData.getBlameMetricName() );
        }
        
    }
    

    public static Map<Tracer, Collection<Tracer>> buildChildren(Collection<Tracer> tracers, List<List<StackTraceElement>> backtraces) {
        if (tracers == null || tracers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Tracer, Collection<Tracer>> children = new HashMap<>();
        for (Tracer tracer : tracers) {
            @SuppressWarnings("unchecked")
            List<StackTraceElement> backtrace = (List<StackTraceElement>) tracer.getAgentAttribute(DefaultTracer.BACKTRACE_PARAMETER_NAME);
            if (null != backtrace) {
                backtraces.add(backtrace);
            }
            
            Tracer parentTracer = tracer.getParentTracer();
            Collection<Tracer> kids = children.get(parentTracer);
            if (kids == null) {
                kids = new ArrayList<>(parentTracer == null ? 1 : Math.max(1, parentTracer.getChildCount()));
                children.put(parentTracer, kids);
            }
            kids.add(tracer);
        }
        return children;
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        Map<String, Object> map = ImmutableMap.<String, Object>of(
                "stack_traces", threadProfiles.asMap(),
                "activity_traces", threadActivityProfiles.asMap());
        JSONObject.writeJSONString(map, out);
    }
    
    private final class MetricNameTime {
        private final MetricName name;
        private final long cpuTime;
        public MetricNameTime(MetricName name, long cpuTime) {
            super();
            this.name = name;
            this.cpuTime = cpuTime;
        }
    }

}
