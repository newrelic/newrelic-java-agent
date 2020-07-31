/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.DataSourceJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;

public class WebsphereLibertyJmxValues extends JmxFrameworkValues {

    private static final int METRIC_COUNT = 1;
    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    public static final String PREFIX = "liberty";

    static {

        //ActiveCount - total number of concurrently active sessions.
        //A session is active if Liberty is processing a request that uses that session.
        METRICS.add(new BaseJmxValue("WebSphere:type=SessionStats,name=*",
                MetricNames.JMX_SESSION + "{name}/",
                new JmxMetric[] { ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("ActiveCount"),
                        ServerJmxMetricGenerator.SESSION_REJECTED_COUNT.createMetric("InvalidatedCount"),
                        ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("InvalidatedCountbyTimeout")
                }));

        METRICS.add(new BaseJmxValue("WebSphere:type=ThreadPoolStats,name=*",
                MetricNames.JMX_THREAD_POOL + "{name}/", new JmxMetric[] {
                ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("ActiveThreads") }));

        METRICS.add(new BaseJmxValue(
                "WebSphere:type=ConnectionPoolStats,name=*",
                MetricNames.JMX_DATASOURCES + "{name}/",
                new JmxMetric[] {
                        DataSourceJmxMetricGenerator.CONNECTIONS_CREATED.createMetric("CreateCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_DESTROYED.createMetric("DestroyCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_MANAGED.createMetric("ManagedConnectionCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_WAIT_TIME.createMetric("WaitTime"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE.createMetric("ConnectionHandleCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_AVAILABLE.createMetric("FreeConnectionCount")
                }));
    }

    public WebsphereLibertyJmxValues() {
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
