/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.threads;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.api.agent.MetricAggregator;

/**
 * This runnable uses the {@link ThreadMXBean} to report metrics about thread cpu states and times.
 */
public class ThreadStateSampler implements Runnable {
    /**
     * A cache of thread ids to some tracked thread state.  The cpu times reported by the Java apis we use are monotonically
     * increasing, so we have to track previous values and compute deltas.
     */
    private final Function<Long, ThreadTracker> threads =
            AgentBridge.collectionFactory.createAccessTimeBasedCache(180, 16, threadId -> new ThreadTracker());
    private final ThreadMXBean threadMXBean;
    private final ThreadNameNormalizer threadNameNormalizer;
    private final MetricAggregator metricAggregator;

    public ThreadStateSampler(ThreadMXBean threadMXBean, ThreadNameNormalizer nameNormalizer) {
        this.threadMXBean = threadMXBean;
        this.metricAggregator = new ThreadStatsMetricAggregator(ServiceFactory.getStatsService());
        this.threadNameNormalizer = nameNormalizer;
    }

    @Override
    public void run() {
        long[] allThreadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(allThreadIds, 0);
        for (ThreadInfo thread : threadInfos) {
            // a thread may terminate after getting its tid but before getting its thread info
            if (thread != null) {
                threads.apply(thread.getThreadId()).update(thread);
            } else {
                Agent.LOG.finer("ThreadStateSampler: Skipping null thread.");
            }
        }
    }

    private class ThreadTracker {

        private long lastThreadTotalCpuTime = -1;
        private long lastThreadUserTime = -1;
        private long lastThreadSystemTime = -1;
        private long lastBlockedTime = -1;
        private long lastWaitedTime = -1;

        private long lastBlockedCount = -1;
        private long lastWaitedCount = -1;

        public ThreadTracker() {
        }

        public void update(ThreadInfo thread) {
            String name = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(thread));

            metricAggregator.recordMetric("Threads/State/" + name + "/" + thread.getThreadState().toString() + "/Count", 1);
            metricAggregator.recordMetric("Threads/SummaryState/" + thread.getThreadState().toString() + "/Count", 1);

            if (threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled()) {
                long totalCpuTime = threadMXBean.getThreadCpuTime(thread.getThreadId());
                long userCpuTime = threadMXBean.getThreadUserTime(thread.getThreadId());
                long systemCpuTime = totalCpuTime - userCpuTime;
                lastThreadTotalCpuTime = recordAccumulatingValue("Threads/TotalTime/" + name + "/CpuTime", totalCpuTime, lastThreadTotalCpuTime, TimeUnit.NANOSECONDS);
                lastThreadUserTime = recordAccumulatingValue("Threads/Time/CPU/" + name + "/UserTime", userCpuTime, lastThreadUserTime, TimeUnit.NANOSECONDS);
                lastThreadSystemTime = recordAccumulatingValue("Threads/Time/CPU/" + name + "/SystemTime", systemCpuTime, lastThreadSystemTime, TimeUnit.NANOSECONDS);
            }
            if (threadMXBean.isThreadContentionMonitoringEnabled()) {
                lastWaitedTime = recordAccumulatingValue("Threads/Time/State/" + name + "/WaitedTime", thread.getWaitedTime(), lastWaitedTime, TimeUnit.MILLISECONDS);
                lastBlockedTime = recordAccumulatingValue("Threads/Time/State/" + name + "/BlockedTime", thread.getBlockedTime(), lastBlockedTime, TimeUnit.MILLISECONDS);
            }

            lastBlockedCount = recordAccumulatingCount("Threads/Count/" + name + "/BlockedCount", thread.getBlockedCount(), lastBlockedCount);
            lastWaitedCount = recordAccumulatingCount("Threads/Count/" + name + "/WaitedCount", thread.getWaitedCount(), lastWaitedCount);
        }

        private long recordAccumulatingCount(String name, long currentCount, long lastCount) {
            try {
                if (currentCount == -1 || lastCount == -1) {
                    return currentCount;
                }
                metricAggregator.recordMetric(name, Math.max(currentCount - lastCount, 0));
            } catch (Exception e) {
                Agent.LOG.log(Level.FINEST, e, e.getMessage());
            }
            return currentCount;
        }

        private long recordAccumulatingValue(String name, long currentTime, long lastTime, TimeUnit timeUnit) {
            try {
                if (currentTime == -1 || lastTime == -1) {
                    return currentTime;
                }
                metricAggregator.recordResponseTimeMetric(name, timeUnit.toMillis(Math.max(currentTime - lastTime, 0)));
            } catch (Exception e) {
                Agent.LOG.log(Level.FINEST, e, e.getMessage());
            }
            return currentTime;
        }

    }

    private class ThreadStatsMetricAggregator extends AbstractMetricAggregator {
        private final StatsService statsService;
        private final boolean isAutoAppNamingEnabled;

        public ThreadStatsMetricAggregator(StatsService statsService) {
            super();
            AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
            isAutoAppNamingEnabled = config.isAutoAppNamingEnabled();
            this.statsService = statsService;
        }

        @Override
        protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
            if (!isAutoAppNamingEnabled) {
                statsService.doStatsWork(new RecordResponseTimeMetricWorker(null, totalTime, exclusiveTime, name, timeUnit), name);
            } else {
                List<IRPMService> rpmServices = ServiceFactory.getRPMServiceManager().getRPMServices();
                RecordResponseTimeMetricWorker responseTimeWorker = new RecordResponseTimeMetricWorker(null, totalTime, exclusiveTime, name, timeUnit);
                for (IRPMService rpmService : rpmServices) {
                    String appName = rpmService.getApplicationName();
                    responseTimeWorker.setAppName(appName);
                    statsService.doStatsWork(responseTimeWorker, name);
                }
            }
        }

        @Override
        protected void doRecordMetric(String name, float value) {
            if (!isAutoAppNamingEnabled) {
                statsService.doStatsWork(new RecordMetricWorker(null, name, value), name);
            } else {
                List<IRPMService> rpmServices = ServiceFactory.getRPMServiceManager().getRPMServices();
                RecordMetricWorker metricWorker = new RecordMetricWorker(null, name, value);
                for (IRPMService rpmService : rpmServices) {
                    String appName = rpmService.getApplicationName();
                    metricWorker.setAppName(appName);
                    statsService.doStatsWork(metricWorker, name);
                }
            }
        }

        @Override
        protected void doIncrementCounter(String name, int count) {
            if (!isAutoAppNamingEnabled) {
                statsService.doStatsWork(new IncrementCounterWorker(null, name, count), name);
            } else {
                List<IRPMService> rpmServices = ServiceFactory.getRPMServiceManager().getRPMServices();
                IncrementCounterWorker incrementCounterWorker = new IncrementCounterWorker(null, name, count);
                for (IRPMService rpmService : rpmServices) {
                    String appName = rpmService.getApplicationName();
                    incrementCounterWorker.setAppName(appName);
                    statsService.doStatsWork(incrementCounterWorker, name);
                }
            }
        }
    }

    private final class RecordMetricWorker implements StatsWork {
        private String appName;
        private final String name;
        private final float value;

        public RecordMetricWorker(String appName, String name, float value) {
            this.appName = appName;
            this.name = name;
            this.value = value;
        }

        @Override
        public void doWork(StatsEngine statsEngine) {
            statsEngine.getStats(name).recordDataPoint(value);
        }

        @Override
        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }
    }

    private final class IncrementCounterWorker implements StatsWork {
        private String appName;
        private final String name;
        private final int count;

        public IncrementCounterWorker(String appName, String name, int count) {
            this.appName = appName;
            this.name = name;
            this.count = count;
        }

        @Override
        public void doWork(StatsEngine statsEngine) {
            statsEngine.getStats(name).incrementCallCount(count);
        }

        @Override
        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }
    }

    private final class RecordResponseTimeMetricWorker implements StatsWork {
        private String appName;
        private final long totalInMillis;
        private final long exclusiveTimeInMillis;
        private final String name;
        private final TimeUnit timeUnit;

        public RecordResponseTimeMetricWorker(String appName, long millis, String name, TimeUnit timeUnit) {
            this(appName, millis, millis, name, timeUnit);
        }

        public RecordResponseTimeMetricWorker(String appName, long totalInMillis, long exclusiveTimeInMillis, String name, TimeUnit timeUnit) {
            this.appName = appName;
            this.exclusiveTimeInMillis = exclusiveTimeInMillis;
            this.totalInMillis = totalInMillis;
            this.timeUnit = timeUnit;
            this.name = name;
        }

        @Override
        public void doWork(StatsEngine statsEngine) {
            statsEngine.getResponseTimeStats(name).recordResponseTime(totalInMillis, exclusiveTimeInMillis, timeUnit);
        }

        @Override
        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }
    }

}
