/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
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

public class EmbeddedTomcatJmxValues extends JmxFrameworkValues {

    /**
     * The Mbean group namespaces are different between standalone Tomcat and Embedded Tomcat
     * Standalone uses the prefix (aka type) Catalina for queries whereas embedded Tomcat uses the prefix (aka type) Tomcat.
     * Our TomcatJmxValues instrumentation only queries MBeans with the Catalina prefix thus queries initiated by embedded Tomcat
     * do not provide metrics. Additionally, datasource metrics
     * were broken out into type: org.apache.tomcat.pool.jmx for embedded tomcat. See (@EmbeddedTomcatDataSourceJmxValues)
     */

    public static final String PREFIX = "Tomcat";

    private static final int METRIC_COUNT = 2;

    // SESSION METRICS (Manager)
    private static final JmxMetric ACTIVE_SESSIONS = ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("activeSessions");
    private static final JmxMetric EXPIRED_SESSIONS = ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("expiredSessions");
    private static final JmxMetric REJECTED_SESSIONS = ServerJmxMetricGenerator.SESSION_REJECTED_COUNT.createMetric("rejectedSessions");
    private static final JmxMetric SESSION_ALIVE_TIME = ServerJmxMetricGenerator.SESSION_AVG_ALIVE_TIME.createMetric("sessionAverageAliveTime");

    // THREAD POOL METRICS
    private static final JmxMetric CURRENT_MAX_COUNT = ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxThreads");
    private static final JmxMetric CURRENT_ACTIVE_COUNT = ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("currentThreadsBusy");
    private static final JmxMetric CURRENT_IDLE_COUNT = JmxMetric.create(new String[] { "currentThreadCount",
            "currentThreadsBusy" }, MetricNames.JMX_THREAD_POOL_IDLE, JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);

    private final List<BaseJmxValue> metrics = new ArrayList<>(METRIC_COUNT);

    public EmbeddedTomcatJmxValues() {
        createMetrics("*");
    }

    public EmbeddedTomcatJmxValues(String name) {
        createMetrics(name);

    }

    private void createMetrics(String name) {
        /*
         * Only used by 7.0+. The manager bean provides information about sessions. sessionCounter is the total number of
         * sessions created by this manager. ActiveSessions is the number of active sessions at this moment.
         * expiredSesions is the number of sessions that have expired. RejectedSessions is the number of sessions
         * rejected due to maxActive being reached. SessionAverageAliveTime is the average time an expired session had
         * been alive.
         */
        metrics.add(new BaseJmxValue(name + ":type=Manager,context=*,host=*,*", MetricNames.JMX_SESSION + "{context}/",
                new JmxMetric[] { ACTIVE_SESSIONS, EXPIRED_SESSIONS, REJECTED_SESSIONS, SESSION_ALIVE_TIME }));

        metrics.add(new BaseJmxValue(name + ":type=ThreadPool,name=*", MetricNames.JMX_THREAD_POOL + "{name}/",
                new JmxMetric[] { CURRENT_ACTIVE_COUNT, CURRENT_IDLE_COUNT, CURRENT_MAX_COUNT }));

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