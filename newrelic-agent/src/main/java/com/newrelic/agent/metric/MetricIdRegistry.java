/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to map {@link MetricName} to ids.
 * 
 * This class is not thread-safe.
 */
public class MetricIdRegistry {

    public static final int METRIC_LIMIT;
    private static final int INITIAL_CAPACITY = 1000;

    static {
        String property = System.getProperty("newrelic.metric_registry_limit");
        METRIC_LIMIT = ((null != property) ? Integer.parseInt(property) : 15000);
    }

    private final Map<MetricName, Integer> metricIds = new HashMap<>(INITIAL_CAPACITY);

    public Integer getMetricId(MetricName metricName) {
        return metricIds.get(metricName);
    }

    public void setMetricId(MetricName metricName, Integer metricId) {
        if (metricIds.size() == METRIC_LIMIT) {
            metricIds.clear();
        }
        metricIds.put(metricName, metricId);
    }

    public void clear() {
        metricIds.clear();
    }

    public int getSize() {
        return metricIds.size();
    }

}
