/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.StatsServiceMetricAggregator;
import com.newrelic.api.agent.MetricAggregator;

import java.text.MessageFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A class to record and harvest metric data.
 *
 * This class is thread-safe.
 */
public class StatsServiceImpl extends AbstractService implements StatsService {
    private final MetricAggregator metricAggregator = new StatsServiceMetricAggregator(this);

    private final ConcurrentMap<String, StatsEngineQueue> statsEngineQueues = new ConcurrentHashMap<>();
    private volatile StatsEngineQueue defaultStatsEngineQueue;
    private final String defaultAppName;

    public StatsServiceImpl() {
        super(StatsService.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        defaultStatsEngineQueue = createStatsEngineQueue();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
        // do nothing
    }

    @Override
    protected void doStop() {
        // do nothing
    }

    @Override
    public void doStatsWork(StatsWork work) {
        String appName = work.getAppName();
        boolean done = false;
        while (!done) {
            done = getOrCreateStatsEngineQueue(appName).doStatsWork(work);
        }
    }

    @Override
    public StatsEngine getStatsEngineForHarvest(String appName) {
        StatsEngineQueue oldStatsEngineQueue = replaceStatsEngineQueue(appName);
        return oldStatsEngineQueue.getStatsEngineForHarvest();
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return metricAggregator;
    }

    private StatsEngineQueue replaceStatsEngineQueue(String appName) {
        StatsEngineQueue oldStatsEngineQueue = getOrCreateStatsEngineQueue(appName);
        StatsEngineQueue newStatsEngineQueue = createStatsEngineQueue();
        if (oldStatsEngineQueue == defaultStatsEngineQueue) {
            defaultStatsEngineQueue = newStatsEngineQueue;
        } else {
            statsEngineQueues.put(appName, newStatsEngineQueue);
        }
        return oldStatsEngineQueue;
    }

    private StatsEngineQueue getOrCreateStatsEngineQueue(String appName) {
        StatsEngineQueue statsEngineQueue = getStatsEngineQueue(appName);
        if (statsEngineQueue != null) {
            return statsEngineQueue;
        }
        statsEngineQueue = createStatsEngineQueue();
        StatsEngineQueue oldStatsEngineQueue = statsEngineQueues.putIfAbsent(appName, statsEngineQueue);
        return oldStatsEngineQueue == null ? statsEngineQueue : oldStatsEngineQueue;
    }

    private StatsEngineQueue getStatsEngineQueue(String appName) {
        if (appName == null || appName.equals(defaultAppName)) {
            return defaultStatsEngineQueue;
        }
        return statsEngineQueues.get(appName);
    }

    private StatsEngineQueue createStatsEngineQueue() {
        return new StatsEngineQueue();
    }

    private static class StatsEngineQueue {

        private final Lock readLock;
        private final Lock writeLock;
        private final AtomicInteger statsEngineCount = new AtomicInteger();

        // reference is guarded by readLock + writeLock
        private ConcurrentLinkedQueue<StatsEngine> statsEngineQueue = new ConcurrentLinkedQueue<>();

        private StatsEngineQueue() {
            ReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();
        }

        public boolean doStatsWork(StatsWork work) {
            if (readLock.tryLock()) {
                try {
                    final Queue<StatsEngine> statsEngineQueue = this.statsEngineQueue;
                    if (statsEngineQueue == null) {
                        //
                        // We've already been harvested.
                        //
                        return false;
                    }
                    doStatsWorkUnderLock(statsEngineQueue, work);
                    return true;
                } finally {
                    readLock.unlock();
                }
            }
            return false;
        }

        private void doStatsWorkUnderLock(Queue<StatsEngine> statsEngineQueue, StatsWork work) {
            StatsEngine statsEngine = null;
            try {
                statsEngine = statsEngineQueue.poll();
                if (statsEngine == null) {
                    statsEngine = createStatsEngine();
                    statsEngineCount.incrementAndGet();
                }
                work.doWork(statsEngine);
            } catch (Exception e) {
                String msg = MessageFormat.format("Exception doing stats work: {0}", e);
                Agent.LOG.warning(msg);
            } finally {
                if (statsEngine != null) {
                    try {
                        if (!statsEngineQueue.offer(statsEngine)) {
                            // should never happen
                            Agent.LOG.warning("Failed to return stats engine to queue");
                        }
                    } catch (Exception e) {
                        // should never happen
                        String msg = MessageFormat.format("Exception returning stats engine to queue: {0}", e);
                        Agent.LOG.warning(msg);
                    }
                }
            }
        }

        public StatsEngine getStatsEngineForHarvest() {
            final Queue<StatsEngine> statsEngineQueue;
            writeLock.lock();
            try {
                statsEngineQueue = this.statsEngineQueue;

                //
                // Clear the reference to the queue so that future calls to doStatsWork() get short-circuited.
                //
                this.statsEngineQueue = null;
            } finally {
                writeLock.unlock();
            }

            //
            // Operations on statsEngineQueue will only occur within an acquired readLock and we've short-circuited
            // any threads that might be racing for it, so safe to do the bulk of the harvest work outside of writeLock.
            //
            return getStatsEngineForHarvest(statsEngineQueue);
        }

        private StatsEngine getStatsEngineForHarvest(Queue<StatsEngine> statsEngines) {
            StatsEngine harvestStatsEngine = createStatsEngine();

            int actualStatsEngineCount = 0;
            for (StatsEngine statsEngine : statsEngines) {
                harvestStatsEngine.mergeStats(statsEngine);
                actualStatsEngineCount++;
            }

            final int expectedStatsEngineCount = statsEngineCount.get();
            if (actualStatsEngineCount != expectedStatsEngineCount) {
                String msg = MessageFormat.format("Error draining stats engine queue. Expected: {0} actual: {1}",
                        expectedStatsEngineCount, actualStatsEngineCount);
                Agent.LOG.warning(msg);
            }

            return harvestStatsEngine;
        }

        private StatsEngine createStatsEngine() {
            return new StatsEngineImpl();
        }
    }

}
