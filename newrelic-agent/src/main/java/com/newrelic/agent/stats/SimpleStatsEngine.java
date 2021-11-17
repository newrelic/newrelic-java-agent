/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class for recording metric stats.
 *
 * This class is thread-safe.
 */
public class SimpleStatsEngine {

    private static final float SCOPED_METRIC_THRESHOLD = 0.02f;

    public static final int DEFAULT_CAPACITY = StatsEngineImpl.DEFAULT_SCOPED_CAPACITY;

    private final Map<String, StatsBase> stats;

    public SimpleStatsEngine() {
        this(DEFAULT_CAPACITY);
    }

    public SimpleStatsEngine(int capacity) {
        stats = new ConcurrentHashMap<>(capacity);
    }

    public Map<String, StatsBase> getStatsMap() {
        return stats;
    }

    public Stats getStats(String metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        StatsBase s = stats.get(metricName);
        if (s == null) {
            s = new StatsImpl();
            stats.put(metricName, s);
        }
        if (s instanceof Stats) {
            return (Stats) s;
        } else {
            String msg = MessageFormat.format("The stats object for {0} is of type {1}", metricName, s.getClass().getName());
            throw new RuntimeException(msg);
        }
    }

    public ResponseTimeStats getOrCreateResponseTimeStats(String metric) {
        if (metric == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        StatsBase s = stats.get(metric);
        if (s == null) {
            s = new ResponseTimeStatsImpl();
            stats.put(metric, s);
        }
        if (s instanceof ResponseTimeStats) {
            return (ResponseTimeStats) s;
        } else {
            String msg = MessageFormat.format("The stats object for {0} is of type {1}", metric, s.getClass().getName());
            throw new RuntimeException(msg);
        }
    }

    public void recordEmptyStats(String metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot record a stat for a null metric");
        }
        stats.put(metricName, AbstractStats.EMPTY_STATS);
    }

    public ApdexStats getApdexStats(String metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        StatsBase s = stats.get(metricName);
        if (s == null) {
            s = new ApdexStatsImpl();
            stats.put(metricName, s);
        }
        if (s instanceof ApdexStats) {
            return (ApdexStats) s;
        } else {
            String msg = MessageFormat.format("The stats object for {0} is of type {1}", metricName, s.getClass().getName());
            throw new RuntimeException(msg);
        }
    }

    public void mergeStats(SimpleStatsEngine other) {
        for (Entry<String, StatsBase> entry : other.stats.entrySet()) {
            StatsBase ourStats = stats.get(entry.getKey());
            StatsBase otherStats = entry.getValue();
            if (ourStats == null) {
                stats.put(entry.getKey(), otherStats);
            } else {
                ourStats.merge(otherStats);
            }
        }
    }

    public void clear() {
        stats.clear();
    }

    public int getSize() {
        return stats.size();
    }

    /**
     * Converts the stats to a list of metric data.
     *
     * @param metricNormalizer The normalizer.
     * @param scope The scope. This should be EMPTY_SCOPE if these are unscoped metrics.
     * @return The list of metric data generated from the internal stats object.
     */
    public List<MetricData> getMetricData(Normalizer metricNormalizer, String scope) {
        List<MetricData> result = new ArrayList<>(stats.size() + 1); // +1 for Java/other
        boolean isTrimStats = ServiceFactory.getConfigService().getDefaultAgentConfig().isTrimStats();

        if (isTrimStats && !scope.equals(MetricName.EMPTY_SCOPE)) {
            trimStats();
        }

        for (Entry<String, StatsBase> entry : stats.entrySet()) {
            MetricName metricName = MetricName.create(entry.getKey(), scope);
            MetricData metricData = createMetricData(metricName, entry.getValue(), metricNormalizer);
            if (metricData != null) {
                result.add(metricData);
            }
        }

        return result;
    }

    protected static MetricData createMetricData(MetricName metricName, StatsBase statsBase, Normalizer metricNormalizer) {
        if (!statsBase.hasData()) {
            return null;
        }

        String normalized = metricNormalizer.normalize(metricName.getName());
        if (normalized == null) {
            return null;
        }

        if (normalized.equals(metricName.getName())) {
            return MetricData.create(metricName, statsBase);
        }

        MetricName normalizedMetricName = MetricName.create(normalized, metricName.getScope());
        return MetricData.create(normalizedMetricName, statsBase);
    }

    private void trimStats() {
        float totalTime = 0;
        for (StatsBase statsBase : stats.values()) {
            ResponseTimeStats stats = (ResponseTimeStats) statsBase;
            totalTime += stats.getTotalExclusiveTime();
        }

        ResponseTimeStatsImpl other = null;
        float threshold = totalTime * SCOPED_METRIC_THRESHOLD;
        Set<String> remove = new HashSet<>();
        for (Entry<String, StatsBase> entry : stats.entrySet()) {
            ResponseTimeStatsImpl statsObj = (ResponseTimeStatsImpl) entry.getValue();
            if (statsObj.getTotalExclusiveTime() < threshold && trimmableMetric(entry.getKey())) {
                if (other == null) {
                    other = statsObj;
                } else {
                    other.merge(statsObj);
                }
                remove.add(entry.getKey());
            }
        }
        if (other != null) {
            stats.put(MetricNames.JAVA_OTHER, other);
            for (String name : remove) {
                stats.remove(name);
            }
        }
    }

    // this is less than awesome and should be cleaned up
    private boolean trimmableMetric(String key) {
        return !(key.startsWith(DatastoreMetrics.METRIC_NAMESPACE) || key.startsWith(MetricNames.EXTERNAL_PATH) ||
                key.startsWith(MetricNames.REQUEST_DISPATCHER) || key.startsWith(MetricNames.GRAPHQL)) ;
    }

    @Override
    public String toString() {
        return "SimpleStatsEngine [stats=" + stats + "]";
    }

}
