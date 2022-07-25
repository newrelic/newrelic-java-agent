/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * A class for recording metric stats.
 *
 * This class is not thread-safe.
 */
public class StatsEngineImpl implements StatsEngine {

    private static final float HASH_SET_LOAD_FACTOR = 0.75f;
    public static final int DEFAULT_CAPACITY = 140;
    public static final int DEFAULT_SCOPED_CAPACITY = 32;
    /**
     * Used to calculate size so to account for the fact that every scoped metric will have an unscoped metric.
     */
    public static final int DOUBLE = 2;

    private final SimpleStatsEngine unscopedStats;
    private final Map<String, SimpleStatsEngine> scopedStats;

    public StatsEngineImpl() {
        this(DEFAULT_CAPACITY);
    }

    public StatsEngineImpl(int capacity) {
        unscopedStats = new SimpleStatsEngine(capacity);
        scopedStats = new HashMap<>(capacity);
    }

    public SimpleStatsEngine getUnscopedStatsForTesting() {
        return unscopedStats;
    }

    public Map<String, SimpleStatsEngine> getScopedStatsForTesting() {
        return scopedStats;
    }

    @Override
    public Stats getStats(String name) {
        return getStats(MetricName.create(name));
    }

    @Override
    public Stats getStats(MetricName metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        return getStatsEngine(metricName).getStats(metricName.getName());
    }

    @Override
    public void recordEmptyStats(String name) {
        recordEmptyStats(MetricName.create(name));
    }

    @Override
    public void recordEmptyStats(MetricName metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot create stats for a null metric");
        }
        getStatsEngine(metricName).recordEmptyStats(metricName.getName());
    }

    private SimpleStatsEngine getStatsEngine(MetricName metricName) {
        if (metricName.isScoped()) {
            SimpleStatsEngine statsEngine = scopedStats.get(metricName.getScope());
            if (statsEngine == null) {
                statsEngine = new SimpleStatsEngine(DEFAULT_SCOPED_CAPACITY);
                scopedStats.put(metricName.getScope(), statsEngine);
            }
            return statsEngine;
        } else {
            return unscopedStats;
        }
    }

    @Override
    public ResponseTimeStats getResponseTimeStats(String name) {
        return getResponseTimeStats(MetricName.create(name));
    }

    @Override
    public ResponseTimeStats getResponseTimeStats(MetricName metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        return getStatsEngine(metricName).getOrCreateResponseTimeStats(metricName.getName());
    }

    @Override
    public ApdexStats getApdexStats(MetricName metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        return getStatsEngine(metricName).getApdexStats(metricName.getName());
    }

    @Override
    public DataUsageStats getDataUsageStats(MetricName metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        return getStatsEngine(metricName).getDataUsageStats(metricName.getName());
    }

    @Override
    public List<MetricName> getMetricNames() {
        List<MetricName> result = new ArrayList<>(getSize());
        for (String name : unscopedStats.getStatsMap().keySet()) {
            result.add(MetricName.create(name));
        }
        for (Entry<String, SimpleStatsEngine> entry : scopedStats.entrySet()) {
            for (String name : entry.getValue().getStatsMap().keySet()) {
                result.add(MetricName.create(name, entry.getKey()));
            }
        }
        return result;
    }

    @Override
    public void clear() {
        unscopedStats.clear();
        scopedStats.clear();
    }

    /**
     * @return the number of metrics tracked by the engine.
     */
    @Override
    public int getSize() {
        int size = unscopedStats.getStatsMap().size();
        for (SimpleStatsEngine engine : scopedStats.values()) {
            size += engine.getStatsMap().size();
        }
        return size;
    }

    @Override
    public void mergeStats(StatsEngine statsEngine) {
        if (statsEngine instanceof StatsEngineImpl) {
            mergeStats((StatsEngineImpl) statsEngine);
        }
    }

    private void mergeStats(StatsEngineImpl other) {
        unscopedStats.mergeStats(other.unscopedStats);
        for (Entry<String, SimpleStatsEngine> entry : other.scopedStats.entrySet()) {
            SimpleStatsEngine scopedStatsEngine = scopedStats.get(entry.getKey());
            if (scopedStatsEngine == null) {
                scopedStatsEngine = new SimpleStatsEngine(entry.getValue().getSize());
                scopedStats.put(entry.getKey(), scopedStatsEngine);
            }
            scopedStatsEngine.mergeStats(entry.getValue());
        }
    }

    @Override
    public void mergeStatsResolvingScope(TransactionStats txStats, String resolvedScope) {
        unscopedStats.mergeStats(txStats.getUnscopedStats());
        if (resolvedScope == null) {
            return;
        }
        SimpleStatsEngine scopedStatsEngine = scopedStats.get(resolvedScope);
        if (scopedStatsEngine == null) {
            scopedStatsEngine = new SimpleStatsEngine(txStats.getScopedStats().getSize());
            scopedStats.put(resolvedScope, scopedStatsEngine);
        }
        scopedStatsEngine.mergeStats(txStats.getScopedStats());
    }

    @Override
    public List<MetricData> getMetricData(Normalizer metricNormalizer) {
        List<MetricData> result = new ArrayList<>(unscopedStats.getStatsMap().size() + (scopedStats.size() * DEFAULT_SCOPED_CAPACITY * DOUBLE));
        for (Entry<String, SimpleStatsEngine> entry : scopedStats.entrySet()) {
            result.addAll(entry.getValue().getMetricData(metricNormalizer, entry.getKey()));
        }
        // add the unscoped to match the scoped
        result.addAll(createUnscopedCopies(metricNormalizer, result));
        // add the other unscoped metrics
        result.addAll(unscopedStats.getMetricData(metricNormalizer, MetricName.EMPTY_SCOPE));

        return result;
    }

    public static List<MetricData> createUnscopedCopies(Normalizer metricNormalizer, final List<MetricData> scopedMetrics) {
        // we do not want to fill up more than 75 percent of the hash map
        // add two to ensure that we are under 75 percent
        int size = (int) (scopedMetrics.size() / 0.75) + 2;
        Map<String, MetricData> allUnscopedMetrics = new HashMap<>(size);
        List<MetricData> results = new ArrayList<>(scopedMetrics.size());

        // iterate through the metrics
        for (MetricData scoped : scopedMetrics) {
            String theMetricName = scoped.getMetricName().getName();
            MetricData unscopedMetric = getUnscopedCloneOfData(metricNormalizer, theMetricName, scoped.getStats());

            if (unscopedMetric != null) {
                // see if we have already seen a metric with this name
                MetricData mapUnscoped = allUnscopedMetrics.get(theMetricName);
                if (mapUnscoped == null) {
                    // add to the results to send out and add to unscoped map to merge additional
                    allUnscopedMetrics.put(theMetricName, unscopedMetric);
                    results.add(unscopedMetric);
                } else {
                    // just add it to the unscoped metric with the same metric name
                    mapUnscoped.getStats().merge(unscopedMetric.getStats());
                }
            }
        }

        return results;
    }

    /**
     * Creates an unscoped metric based on the input parameters. The stats will be cloned.
     *
     * @param metricNormalizer The normalizer.
     * @param metricName The name of the metric.
     * @param stats The stats.
     * @return The newly created unscoped metric.
     */
    private static MetricData getUnscopedCloneOfData(Normalizer metricNormalizer, String metricName, StatsBase stats) {
        if (stats != null) {
            MetricName metricNameUnscoped = MetricName.create(metricName);
            try {
                MetricData metricDataUnscoped = SimpleStatsEngine.createMetricData(metricNameUnscoped, (StatsBase) stats.clone(), metricNormalizer);
                return metricDataUnscoped;
            } catch (CloneNotSupportedException e) {
                Agent.LOG.log(Level.INFO, "Unscoped metric not created because stats base could not be cloned for " + metricNameUnscoped.getName());
                return null;
            }
        } else {
            return null;
        }
    }

}
