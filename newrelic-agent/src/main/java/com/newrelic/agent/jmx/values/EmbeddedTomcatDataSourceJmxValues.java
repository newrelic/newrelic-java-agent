/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.DataSourceJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;

import java.util.ArrayList;
import java.util.List;

public class EmbeddedTomcatDataSourceJmxValues extends JmxFrameworkValues {

    /**
     * The Mbean group namespaces are different between standalone Tomcat and Embedded Tomcat
     * Standalone uses the prefix (aka type) Catalina for queries whereas embedded Tomcat uses the prefix (aka type) Tomcat.
     * Our TomcatJmxValues instrumentation only queries MBeans with the Catalina prefix thus queries initiated by embedded Tomcat
     * do not provide metrics. Additionally, datasource metrics
     * were broken out into type: org.apache.tomcat.pool.jmx for embedded tomcat, hence this Class. See (@EmbeddedTomcatJmxValues)
     */

    public static final String PREFIX = "org.apache.tomcat.jdbc.pool.jmx";

    private static final int METRIC_COUNT = 1;

    private static final JmxMetric CONNECTIONS_ACTIVE = DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE.createMetric("NumActive");
    private static final JmxMetric CONNECTIONS_IDLE = DataSourceJmxMetricGenerator.CONNECTIONS_IDLE.createMetric("NumIdle");
    private static final JmxMetric CONNECTIONS_MAX = DataSourceJmxMetricGenerator.CONNECTIONS_MAX.createMetric("MaxActive");
    private static final JmxMetric CONNECTIONS_CREATED = DataSourceJmxMetricGenerator.CONNECTIONS_CREATED.createMetric("CreatedCount");

    private final List<BaseJmxValue> metrics = new ArrayList<>(METRIC_COUNT);

    public EmbeddedTomcatDataSourceJmxValues() {
        createMetrics("*");
    }

    public EmbeddedTomcatDataSourceJmxValues(String name) {
        createMetrics(name);

    }

    private void createMetrics(String name) {

        metrics.add(new BaseJmxValue("org.apache.tomcat.jdbc.pool.jmx:name=*,type=ConnectionPool", MetricNames.JMX_DATASOURCES + "{name}/",
                new JmxMetric[] { CONNECTIONS_ACTIVE, CONNECTIONS_IDLE, CONNECTIONS_MAX, CONNECTIONS_CREATED }));
    }

    @Override
    public List<BaseJmxValue> getFrameworkMetrics() {
        return metrics;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}