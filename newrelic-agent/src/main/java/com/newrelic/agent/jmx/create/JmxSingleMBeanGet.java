/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;

public class JmxSingleMBeanGet extends JmxGet {

    /**
     * 
     * Creates this JmxGet.
     * 
     * @param pObjectName The object name.
     * @param safeName The safeName for the metric.
     * @param pAttributesToType The attributes corresponding with the type.
     * @throws MalformedObjectNameException Thrown if a problem with the object name.
     */
    public JmxSingleMBeanGet(String pObjectName, String rootMetricName, String safeName,
            Map<JmxType, List<String>> pAttributesToType, Extension origin) throws MalformedObjectNameException {
        super(pObjectName, rootMetricName, safeName, pAttributesToType, origin);
    }

    /**
     * 
     * Creates this JmxGet.
     */
    public JmxSingleMBeanGet(String pObjectName, String safeName, String pRootMetric, List<JmxMetric> pMetrics,
            JmxAttributeFilter attributeFilter, JmxMetricModifier modifier) throws MalformedObjectNameException {
        super(pObjectName, safeName, pRootMetric, pMetrics, attributeFilter, modifier);
    }

    @Override
    public void recordStats(StatsEngine statsEngine, Map<ObjectName, Map<String, Float>> resultingMetricToValue,
            MBeanServer server) {
        String actualRootMetricName;
        for (Entry<ObjectName, Map<String, Float>> currentMBean : resultingMetricToValue.entrySet()) {
            actualRootMetricName = getRootMetricName(currentMBean.getKey(), server);
            // the logging is in the keep metric
            if (actualRootMetricName.length() > 0
                    && (getJmxAttributeFilter() == null || getJmxAttributeFilter().keepMetric(actualRootMetricName))) {
                for (JmxMetric current : getJmxMetrics()) {
                    current.recordSingleMBeanStats(statsEngine, actualRootMetricName, currentMBean.getValue());
                }
            }
        }
    }
}
