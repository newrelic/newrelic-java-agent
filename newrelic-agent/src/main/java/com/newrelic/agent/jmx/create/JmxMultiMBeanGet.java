/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JmxMultiMBeanGet extends JmxGet {

    /**
     * 
     * Creates this JmxMultiMBeanMetric.
     * 
     * @param pObjectName The object name.
     * @param safeName The safeName for the metric.
     * @param pAttributesToType The attributes corresponding with the type.
     * @throws MalformedObjectNameException Thrown if a problem with the object name.
     */
    public JmxMultiMBeanGet(String pObjectName, String rootMetricName, String safeName,
            Map<JmxType, List<String>> pAttributesToType, Extension origin) throws MalformedObjectNameException {
        super(pObjectName, rootMetricName, safeName, pAttributesToType, origin);
    }

    /**
     * 
     * Creates this JmxMultiMBeanMetric.
     */
    public JmxMultiMBeanGet(String pObjectName, String safeName, String pRootMetric, List<JmxMetric> pMetrics,
            JmxAttributeFilter attributeFilter, JmxMetricModifier modifier) throws MalformedObjectNameException {
        super(pObjectName, safeName, pRootMetric, pMetrics, attributeFilter, modifier);
    }

    @Override
    public void recordStats(StatsEngine statsEngine, Map<ObjectName, Map<String, Float>> resultingMetricToValue,
            MBeanServer server) {
        String actualRootMetricName;

        Map<ObjectName, String> rootMetricNames = new HashMap<>();
        for (JmxMetric currentMetric : getJmxMetrics()) {
            Map<String, Float> mbeansWithValues = new HashMap<>();
            for (Entry<ObjectName, Map<String, Float>> currentMBean : resultingMetricToValue.entrySet()) {
                actualRootMetricName = rootMetricNames.get(currentMBean.getKey());
                if (actualRootMetricName == null) {
                    actualRootMetricName = getRootMetricName(currentMBean.getKey(), server);
                }
                currentMetric.applySingleMBean(actualRootMetricName, currentMBean.getValue(), mbeansWithValues);
            }
            currentMetric.recordMultMBeanStats(statsEngine, mbeansWithValues);
        }
    }
}
