/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SolrMetricCollector {

    private static final AtomicBoolean init = new AtomicBoolean();

    public static void initialize() {
        if (init.compareAndSet(false, true)) {
            AgentBridge.instrumentation.registerCloseable(Weaver.getImplementationTitle(),
                    AgentBridge.privateApi.addSampler(getSolrJmxSampler(), 1, TimeUnit.MINUTES));
        }
    }

    private static Runnable getSolrJmxSampler() {
        return new Runnable() {
            @Override
            public void run() {
                ConcurrentHashMap<String, NRMetric> metrics = MetricUtil.getMetrics();
                Set<String> keys = metrics.keySet();
                for (String key : keys) {
                    NRMetric metric = metrics.get(key);
                    if (metric != null) {
                        metric.reportMetrics();
                    }
                }
            }
        };
    }

}
