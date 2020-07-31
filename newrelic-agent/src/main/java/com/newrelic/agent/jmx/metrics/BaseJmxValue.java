/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import java.util.Arrays;
import java.util.List;

import com.newrelic.agent.jmx.create.JmxAttributeFilter;
import com.newrelic.agent.jmx.create.JmxMetricModifier;

/**
 * This is the base class for storing a jmx metrics.
 * 
 * @since Mar 6, 2013
 */
public class BaseJmxValue {

    /** The object name for the jmx metric. */
    private final String objectNameString;
    /**
     * The root name of the metric. The attribute name will be added. Set this to null to use the default naming
     * mechanism.
     */
    private final String objectMetricName;
    /** The metric format . */
    private final List<JmxMetric> metrics;
    private final JmxAttributeFilter attributeFilter;
    private final JMXMetricType type;
    private final JmxMetricModifier modifier;

    public BaseJmxValue(final String pObjectName, final String pObjectMetricName, JmxMetric[] pMetrics) {
        this(pObjectName, pObjectMetricName, null, null, JMXMetricType.INCREMENT_COUNT_PER_BEAN, pMetrics);
    }

    public BaseJmxValue(final String pObjectName, final String pObjectMetricName, JmxAttributeFilter attributeFilter,
            JmxMetric[] pMetrics) {
        this(pObjectName, pObjectMetricName, attributeFilter, null, JMXMetricType.INCREMENT_COUNT_PER_BEAN, pMetrics);
    }

    public BaseJmxValue(final String pObjectName, final String pObjectMetricName, JmxMetricModifier pModifier,
            JmxMetric[] pMetrics) {
        this(pObjectName, pObjectMetricName, null, pModifier, JMXMetricType.INCREMENT_COUNT_PER_BEAN, pMetrics);
    }

    public BaseJmxValue(final String pObjectName, final String pObjectMetricName, JmxAttributeFilter attributeFilter,
            JmxMetricModifier pModifier, JMXMetricType pType, JmxMetric[] pMetrics) {
        objectNameString = pObjectName;
        // set this to null to use the default naming mechanism
        objectMetricName = pObjectMetricName;

        metrics = Arrays.asList(pMetrics);
        this.attributeFilter = attributeFilter;
        type = pType;
        modifier = pModifier;
    }

    /**
     * Gets the field objectNameString.
     * 
     * @return the objectNameString
     */
    public String getObjectNameString() {
        return objectNameString;
    }

    /**
     * Gets the field objectMetricName.
     * 
     * @return the objectMetricName
     */
    public String getObjectMetricName() {
        return objectMetricName;
    }

    /**
     * Gets the field metrics.
     * 
     * @return the metrics
     */
    public List<JmxMetric> getMetrics() {
        return metrics;
    }

    public JmxAttributeFilter getAttributeFilter() {
        return attributeFilter;
    }

    public JMXMetricType getType() {
        return type;
    }

    public JmxMetricModifier getModifier() {
        return modifier;
    }

}
