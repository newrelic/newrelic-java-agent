/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxInit;
import com.newrelic.agent.jmx.metrics.JmxMetric;

import java.util.ArrayList;
import java.util.List;

/**
 * These metrics are loaded on startup. This is why the JmxInit annotation is present.
 * 
 * @since Mar 12, 2013
 */
@JmxInit
public class JavaLangJmxMetrics extends JmxFrameworkValues {

    private static String PREFIX = "java.lang";

    private static final int METRIC_COUNT = 2;
    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    // THREAD METRICS
    private static final JmxMetric CURRENT_THREAD_COUNT = JmxMetric.create("ThreadCount", MetricNames.JMX_THREAD_COUNT,
            JmxType.SIMPLE);
    private static final JmxMetric TOTAL_THREAD_COUNT = JmxMetric.create("TotalStartedThreadCount",
            MetricNames.JMX_THREAD_TOTAL_COUNT, JmxType.SIMPLE);

    // CLASSLOADING METRICS
    private static final JmxMetric LOADED_CLASSES = JmxMetric.create("LoadedClassCount",
            MetricNames.JMX_LOADED_CLASSES, JmxType.SIMPLE);
    private static final JmxMetric UNLOADED_CLASSES = JmxMetric.create("UnloadedClassCount",
            MetricNames.JMX_UNLOADED_CLASSES, JmxType.SIMPLE);

    static {
        /*
         * Threading information from the JVM. ThreadCount is the current number of live threads including daemon and
         * non-daemon threads. TotalStartedThreadCount is the total number of threads created since JVM started.
         */
        METRICS.add(new BaseJmxValue("java.lang:type=Threading", MetricNames.JMX_THREAD, new JmxMetric[] {
                CURRENT_THREAD_COUNT, TOTAL_THREAD_COUNT }));

        /*
         * Classloading information from the JVM. LoadedClassCount is the number of classes that are currently loaded in
         * the JVM. Total number of classes unloaded since the JVM started execution.
         */
        METRICS.add(new BaseJmxValue("java.lang:type=ClassLoading", MetricNames.JMX_CLASSES, new JmxMetric[] {
                LOADED_CLASSES, UNLOADED_CLASSES }));

    }

    public JavaLangJmxMetrics() {
        super();
    }

    @Override
    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
