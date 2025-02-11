/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.api.agent.NewRelic;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class is thread-safe.
 */
public class GCService extends AbstractService implements HarvestListener {

    public GCService() {
        super(GCService.class.getSimpleName());
    }

    private final Map<String, GarbageCollector> garbageCollectors = new HashMap<>();

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    @Override
    protected void doStop() {

    }

    @Override
    public synchronized void beforeHarvest(String appName, StatsEngine statsEngine) {
        try {
            harvestGC(statsEngine);
        } catch (Exception e) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Error harvesting GC metrics for {0}: {1}", appName, e);
                Agent.LOG.finer(msg);
            }
        }
    }

    private void harvestGC(StatsEngine statsEngine) {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "GC Debug: Found GC Bean {0} with collection count {1}", gcBean.getName(), gcBean.getCollectionCount());
            GarbageCollector garbageCollector = garbageCollectors.get(gcBean.getName());
            if (garbageCollector == null) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "GC Debug: registering new garbage collector {0}", gcBean.getName());
                garbageCollector = new GarbageCollector(gcBean);
                garbageCollectors.put(gcBean.getName(), garbageCollector);
            }
            NewRelic.getAgent().getLogger().log(Level.INFO, "GC Debug: There are {0} collectors registered", garbageCollectors.size());
            // tmp modification: move out of else block
            // to test possible edge case
            garbageCollector.recordGC(gcBean, statsEngine);
        }
    }

    @Override
    public void afterHarvest(String appName) {
        // ignore
    }

    private class GarbageCollector {

        private long collectionCount;
        private long collectionTime;

        public GarbageCollector(GarbageCollectorMXBean gcBean) {
            collectionCount = gcBean.getCollectionCount();
            collectionTime = gcBean.getCollectionTime();
        }

        private void recordGC(GarbageCollectorMXBean gcBean, StatsEngine statsEngine) {
            final long lastCollectionCount = collectionCount;
            final long lastCollectionTime = collectionTime;

            collectionCount = gcBean.getCollectionCount();
            collectionTime = gcBean.getCollectionTime();

            final long numberOfCollections = collectionCount - lastCollectionCount;
            final long time = collectionTime - lastCollectionTime;

            if (numberOfCollections > 0) {
                String rootMetricName = "GC/" + gcBean.getName();
                ResponseTimeStats stats = statsEngine.getResponseTimeStats(rootMetricName);
                stats.recordResponseTime(time, TimeUnit.MILLISECONDS);
                stats.setCallCount((int) numberOfCollections);
            }
        }
    }

}
