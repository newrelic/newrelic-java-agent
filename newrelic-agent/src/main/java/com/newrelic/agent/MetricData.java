/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.stats.StatsBase;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is thread-safe.
 */
public class MetricData implements JSONStreamAware {

    private final MetricName metricName;
    private final Integer metricId;
    private final StatsBase stats;

    private MetricData(MetricName metricName, Integer metricId, StatsBase stats) {
        this.stats = stats;
        this.metricId = metricId;
        this.metricName = metricName;
    }

    public StatsBase getStats() {
        return stats;
    }

    public MetricName getMetricName() {
        return metricName;
    }

    public Integer getMetricId() {
        return metricId;
    }

    public Object getKey() {
        return metricId != null ? metricId : metricName;
    }

    @Override
    public String toString() {
        return metricName.toString();
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        List<Object> result = new ArrayList<>(2);
        if (metricId == null) {
            result.add(metricName);
        } else {
            result.add(metricId);
        }
        result.add(stats);
        JSONArray.writeJSONString(result, writer);
    }

    public static MetricData create(MetricName metricName, StatsBase stats) {
        return create(metricName, null, stats);
    }

    public static MetricData create(MetricName metricName, Integer metricId, StatsBase stats) {
        return new MetricData(metricName, metricId, stats);
    }

}
