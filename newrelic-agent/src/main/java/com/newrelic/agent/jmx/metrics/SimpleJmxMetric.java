/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.stats.StatsEngine;

public class SimpleJmxMetric extends JmxMetric {

    public SimpleJmxMetric(String attribute) {
        super(attribute);
    }

    public SimpleJmxMetric(String[] attributes, String attName, JmxAction pAction) {
        super(attributes, attName, pAction);
    }

    @Override
    public JmxType getType() {
        return JmxType.SIMPLE;
    }

    @Override
    public void recordStats(StatsEngine statsEngine, String metricName, float value) {
        statsEngine.getStats(metricName).recordDataPoint(value);
    }

}
