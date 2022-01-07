/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

import com.codahale.metrics.Gauge;
import com.newrelic.api.agent.NewRelic;
import org.apache.solr.core.SolrInfoBean;

public class GaugeMetric extends NRMetric {

    @SuppressWarnings("rawtypes")
    Gauge metric;
    String metricType;
    String metricName;

    @SuppressWarnings("rawtypes")
    public GaugeMetric(String mn, String mt, String r, Gauge m, String b) {
        super(r, b);
        metric = m;
        metricType = mt;
        metricName = mn;
    }

    @Override
    public String getMetricName(String name) {
        return getMetricBase() + "/" + name;
    }

    @Override
    public String getMetricBase() {
        return prefix + registry + "/" + metricType + "/" + name;
    }

    @Override
    public int reportMetrics() {
        int numMetrics = 0;
        Object obj = metric.getValue();
        if (Number.class.isInstance(obj)) {
            Number num = (Number) obj;
            NewRelic.recordMetric(getMetricName(metricName), num.floatValue());
            numMetrics++;
        }
        return numMetrics;
    }

}
