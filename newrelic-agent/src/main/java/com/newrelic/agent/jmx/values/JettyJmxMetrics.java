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
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;

public class JettyJmxMetrics extends JmxFrameworkValues {

    public static final String PREFIX = "org.eclipse.jetty";

    private static final int METRIC_COUNT = 1;
    private static List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    private static final JmxMetric CURRENT_MAX_COUNT = ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxThreads");
    private static final JmxMetric CURRENT_IDLE_COUNT = ServerJmxMetricGenerator.IDLE_THREAD_POOL_COUNT.createMetric("idleThreads");
    private static final JmxMetric CURRENT_ACTIVE_COUNT = JmxMetric.create(new String[] { "threads", "idleThreads" },
            MetricNames.JMX_THREAD_POOL_ACTIVE, JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);

    static {
        /*
         * Provides threading information. threads is the number of threads in the pool. IdleThreads is the number of
         * idle threads in the pool. This has been tested in jetty 9.0, jetty 8.1, and jetty 7.3.
         */
        METRICS.add(new BaseJmxValue("org.eclipse.jetty.util.thread:type=queuedthreadpool,id=*",
                MetricNames.JMX_THREAD_POOL + "{id}/", new JmxMetric[] { CURRENT_IDLE_COUNT, CURRENT_ACTIVE_COUNT,
                        CURRENT_MAX_COUNT }));

    }

    public JettyJmxMetrics() {
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
