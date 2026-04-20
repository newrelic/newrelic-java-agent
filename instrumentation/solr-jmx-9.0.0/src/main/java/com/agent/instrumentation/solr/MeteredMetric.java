/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

import com.codahale.metrics.Metered;
import com.newrelic.api.agent.NewRelic;
import org.apache.solr.core.SolrInfoBean;

public class MeteredMetric extends NRMetric {

    Metered metered;
    String metricType;
    String metricName;

    public MeteredMetric(String mn, String mt, String r, String b, Metered m) {
        super(r, b);
        metered = m;
        metricType = mt;
        metricName = mn;
    }

    public MeteredMetric(String mn, String mt, String r, String b, Metered m, String tag) {
        super(r, b);
        metered = m;
        metricType = mt;
        metricName = mn;
        this.contextTag = tag;
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
        long count = metered.getCount();
        String fullMetricName = getMetricName(metricName);
        NewRelic.recordMetric(fullMetricName, count);
        return 1;
    }

}
