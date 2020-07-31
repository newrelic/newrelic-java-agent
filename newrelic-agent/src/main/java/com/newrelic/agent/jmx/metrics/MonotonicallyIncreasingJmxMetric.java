/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.stats.MonotonicallyIncreasingStatsEngine;
import com.newrelic.agent.stats.StatsEngine;

public class MonotonicallyIncreasingJmxMetric extends JmxMetric {

    private static final MonotonicallyIncreasingStatsEngine monoStatsEngine = new MonotonicallyIncreasingStatsEngine();

    public MonotonicallyIncreasingJmxMetric(String attribute) {
        super(attribute);
    }

    public MonotonicallyIncreasingJmxMetric(String[] attributes, String attName, JmxAction pAction) {
        super(attributes, attName, pAction);
    }

    @Override
    public JmxType getType() {
        return JmxType.MONOTONICALLY_INCREASING;
    }

    @Override
    public void recordStats(StatsEngine statsEngine, String name, float value) {
        monoStatsEngine.recordMonoStats(statsEngine, name, value);
    }

}
