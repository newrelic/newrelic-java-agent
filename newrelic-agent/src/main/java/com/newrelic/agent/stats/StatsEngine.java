/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;

import java.util.List;

/**
 * This class maps {@link MetricName} objects to {@link Stats} objects.
 */
public interface StatsEngine {

    Stats getStats(String name);

    Stats getStats(MetricName metric);

    /**
     * Send a zero-count metric to apm. If this metric is already present, it will be zeroed out.
     *
     * @param name Name of the metric
     */
    void recordEmptyStats(String name);

    /**
     * Send a zero-count metric to apm.
     *
     * @param metricName Name of the metric. If this metric is already present, it will be zeroed out.
     */
    void recordEmptyStats(MetricName metricName);

    ResponseTimeStats getResponseTimeStats(String name);

    ResponseTimeStats getResponseTimeStats(MetricName metric);

    ApdexStats getApdexStats(MetricName metric);

    DataUsageStats getDataUsageStats(MetricName metric);

    /**
     * This is now only used by tests.
     */
    List<MetricName> getMetricNames();

    void clear();

    List<MetricData> getMetricData(Normalizer metricNormalizer);

    void mergeStats(StatsEngine statsEngine);

    void mergeStatsResolvingScope(TransactionStats statsEngine, String resolvedScope);

    int getSize();

}
