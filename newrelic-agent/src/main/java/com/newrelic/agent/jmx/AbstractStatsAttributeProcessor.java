/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import java.util.concurrent.TimeUnit;

import com.newrelic.agent.MetricNames;

public abstract class AbstractStatsAttributeProcessor implements JmxAttributeProcessor {

    /**
     * We are only going to add the float to the output if the metric name starts with JmxBuiltIn. JmxBuiltIn is the
     * name for custom JMX metrics which are being set for a specific graph. If the metric name starts with Jmx/ then we
     * want to keep with the old tradition of recording the metrics now. I realize this a little quarky but am doing it
     * to keep old functionality and add the new functionality.
     * 
     * @return True if the float result should be added to the output map.
     */
    protected static boolean isBuiltInMetric(final String metricName) {
        return metricName != null && metricName.startsWith(MetricNames.JMX_CUSTOM);
    }

    protected static TimeUnit getTimeUnit(String unit) {
        if ("HOUR".equals(unit)) {
            return TimeUnit.HOURS;
        } else if ("MINUTE".equals(unit)) {
            return TimeUnit.MINUTES;
        } else if ("SECOND".equals(unit)) {
            return TimeUnit.SECONDS;
        } else if ("MILLISECOND".equals(unit)) {
            return TimeUnit.MILLISECONDS;
        } else if ("MICROSECOND".equals(unit)) {
            return TimeUnit.MICROSECONDS;
        } else if ("NANOSECOND".equals(unit)) {
            return TimeUnit.NANOSECONDS;
        }

        return TimeUnit.MILLISECONDS;
    }

}
