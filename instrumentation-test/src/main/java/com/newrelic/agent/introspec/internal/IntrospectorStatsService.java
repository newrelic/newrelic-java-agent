/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.normalization.NormalizationRule;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.normalization.NormalizerImpl;
import com.newrelic.agent.stats.CountStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.StatsWork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class IntrospectorStatsService extends StatsServiceImpl implements StatsService {

    static final String TEST_APP_NAME = "TEST_APP_NAME";
    private final StatsEngineImpl engine = new StatsEngineImpl();
    private final Normalizer metricNormalizer = new NormalizerImpl(TEST_APP_NAME, Collections.<NormalizationRule>emptyList());

    protected IntrospectorStatsService() {
        super();
    }

    @Override
    public void doStatsWork(StatsWork statsWork) {
        synchronized (this) {
            statsWork.doWork(engine);
        }
    }

    public Map<String, TracedMetricData> getUnscopedMetrics() {
        SimpleStatsEngine unscoped = engine.getUnscopedStatsForTesting();

        Map<String, TracedMetricData> unscopedMetricMap = convertToTracedMetric(unscoped);

        // copies of scoped metrics -> unscoped
        List<MetricData> allScopedMetricData = new ArrayList<>();
        for (Entry<String, SimpleStatsEngine> entry : engine.getScopedStatsForTesting().entrySet()) {
            allScopedMetricData.addAll(entry.getValue().getMetricData(metricNormalizer, entry.getKey()));
        }
        List<MetricData> unscopedCopies = StatsEngineImpl.createUnscopedCopies(metricNormalizer, allScopedMetricData);
        for (MetricData unscopedCopy : unscopedCopies) {
            if (unscopedCopy.getStats() instanceof CountStats) {
                String metricName = unscopedCopy.getMetricName().getName();
                TracedMetricData data = TracedMetricImpl.getTracedMetricFromStatsBase(metricName, (CountStats) unscopedCopy.getStats());
                unscopedMetricMap.put(metricName, data);
            }
        }

        return unscopedMetricMap;
    }

    public Map<String, TracedMetricData> getScopedMetrics(String transactionName) {
        Map<String, SimpleStatsEngine> data = engine.getScopedStatsForTesting();
        SimpleStatsEngine txEngine = data.get(transactionName);
        if (txEngine == null) {
            throw new IllegalArgumentException("no such transaction: " + transactionName);
        }
        return convertToTracedMetric(txEngine);
    }

    public void clear() {
        engine.clear();
    }

    private Map<String, TracedMetricData> convertToTracedMetric(SimpleStatsEngine currentEngine) {
        Map<String, TracedMetricData> output = Maps.newHashMapWithExpectedSize(currentEngine.getSize());
        for (Entry<String, StatsBase> current : currentEngine.getStatsMap().entrySet()) {
            if (current.getValue() instanceof CountStats) {
                TracedMetricData data = TracedMetricImpl.getTracedMetricFromStatsBase(current.getKey(), (CountStats) current.getValue());
                output.put(current.getKey(), data);
            }
        }
        return output;
    }

}
