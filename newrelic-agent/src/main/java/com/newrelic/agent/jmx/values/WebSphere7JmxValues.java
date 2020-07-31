/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.JtaJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * The web sphere metrics are handled differently from the other application servers by the Jmx Service. For example,
 * the general ones under the JMX name space do not appear to every allow for monotomically increasing metrics.
 * 
 * In short, go look at the code before adding a metric here. Make sure the metric will be handled correctly.
 */
public class WebSphere7JmxValues extends JmxFrameworkValues {

    private static final int METRIC_COUNT = 2;

    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    public static final String PREFIX = "WebSphere-7";

    static {
        /* Returns the active count and max size for each thread pool. */
        METRICS.add(new BaseJmxValue("WebSphere:type=ThreadPool,name=*,process=*,platform=*,node=*,*",
                MetricNames.JMX_THREAD_POOL + "{name}/", new JmxMetric[] {
                        ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("stats.ActiveCount"),
                        ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maximumSize") }));

        METRICS.add(new BaseJmxValue(
                "WebSphere:j2eeType=JTAResource,type=TransactionService,name=*,process=*,platform=*,node=*,*",
                MetricNames.JMX_JTA + "{type}/", new JmxMetric[] {
                        JtaJmxMetricGenerator.COMMIT.createMetric("stats.CommittedCount"),
                        JtaJmxMetricGenerator.ROLLBACK.createMetric("stats.RolledbackCount"),
                        JtaJmxMetricGenerator.TIMEOUT.createMetric("stats.GlobalTimeoutCount") }));

        /* Returns the LiveCount which is the number of active sessions. */
        METRICS.add(new BaseJmxValue("WebSphere:type=SessionManager,name=*,process=*,platform=*,node=*,*",
                MetricNames.JMX_SESSION + "{name}/",
                new JmxMetric[] { ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("stats.LiveCount") }));

    }

    public WebSphere7JmxValues() {
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
