/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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
                NewRelic.recordMetric(getMetricName(key), num.floatValue());
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
