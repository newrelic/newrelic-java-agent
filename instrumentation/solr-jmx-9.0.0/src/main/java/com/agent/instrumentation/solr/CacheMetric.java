/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

import com.codahale.metrics.Metric;
import com.newrelic.api.agent.NewRelic;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.MetricsMap;

import java.util.Map;

public class CacheMetric extends NRMetric {

    MetricsMap metric = null;
    String metricType = null;

    public CacheMetric(String mt, String r, Metric m, String b) {
        super(r, b);
        metricType = mt;
        if (MetricsMap.class.isInstance(m)) {
            metric = (MetricsMap) m;
        }
    }

    public CacheMetric(String mt, String r, Metric m, String b, String tag) {
        super(r, b);
        metricType = mt;
        if (MetricsMap.class.isInstance(m)) {
            metric = (MetricsMap) m;
        }
        this.contextTag = tag;
    }

    @Override
    public String getMetricName(String name) {
        return getMetricBase() + "/" + name;
    }

    @Override
    public int reportMetrics() {
        int numMetrics = 0;
        Map<String, Object> map = metric.getValue();
        for (String key : map.keySet()) {
            Object obj = map.get(key);
            if (Number.class.isInstance(obj)) {
                Number num = (Number) obj;
                String fullMetricName = getMetricName(key);
                NewRelic.recordMetric(fullMetricName, num.floatValue());
                numMetrics++;
            }
        }
        return numMetrics;
    }

    @Override
    public String getMetricBase() {
        return prefix + registry + "/" + metricType + "/" + name;
    }

}
