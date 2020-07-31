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
import com.newrelic.agent.jmx.metrics.DataSourceJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;

public class TomcatJmxValues extends JmxFrameworkValues {

    /**
     * This is the main prefix for JMX metrics. However, the prefix can be changed by setting the name of the engine to
     * something else.
     */
    public static final String PREFIX = "Catalina";

    private static final int METRIC_COUNT = 3;

    // SESSION METRICS
    private static final JmxMetric ACTIVE_SESSIONS = ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("activeSessions");
    private static final JmxMetric EXPIRED_SESSIONS = ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("expiredSessions");
    private static final JmxMetric REJECTED_SESSIONS = ServerJmxMetricGenerator.SESSION_REJECTED_COUNT.createMetric("rejectedSessions");
    private static final JmxMetric SESSION_ALIVE_TIME = ServerJmxMetricGenerator.SESSION_AVG_ALIVE_TIME.createMetric("sessionAverageAliveTime");

    private static final JmxMetric CONNECTIONS_ACTIVE = DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE.createMetric("numActive");
    private static final JmxMetric CONNECTIONS_IDLE = DataSourceJmxMetricGenerator.CONNECTIONS_IDLE.createMetric("numIdle");
    private static final JmxMetric CONNECTIONS_MAX = DataSourceJmxMetricGenerator.CONNECTIONS_MAX.createMetric("maxActive");
    private static final JmxMetric CONNECTIONS_MAX_TOMCAT_8 = DataSourceJmxMetricGenerator.CONNECTIONS_MAX.createMetric("maxTotal");


    // THREAD POOL METRICS
    private static final JmxMetric CURRENT_MAX_COUNT = ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxThreads");
    private static final JmxMetric CURRENT_ACTIVE_COUNT = ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("currentThreadsBusy");
    private static final JmxMetric CURRENT_IDLE_COUNT = JmxMetric.create(new String[] { "currentThreadCount",
            "currentThreadsBusy" }, MetricNames.JMX_THREAD_POOL_IDLE, JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);

    private final List<BaseJmxValue> metrics = new ArrayList<>(METRIC_COUNT);

    public TomcatJmxValues() {
        createMetrics("*");
    }

    public TomcatJmxValues(String name) {
        createMetrics(name);

    }

    private void createMetrics(String name) {
        /*
         * Only used by 7.0. The manager bean provides information about sessions. sessionCounter is the total number of
         * sessions created by this manager. ActiveSessions is the number of active sessions at this moment.
         * expiredSesions is the number of sessions that have expired. RejectedSessions is the number of sessions
         * rejected due to maxActive being reached. SessionAverageAliveTime is the average time an expired session had
         * been alive.
         */
        metrics.add(new BaseJmxValue(name + ":type=Manager,context=*,host=*,*", MetricNames.JMX_SESSION + "{context}/",
                new JmxMetric[] { ACTIVE_SESSIONS, EXPIRED_SESSIONS, REJECTED_SESSIONS, SESSION_ALIVE_TIME }));
        /* This is for 6.0 and 5.5. */
        metrics.add(new BaseJmxValue(name + ":type=Manager,path=*,host=*", MetricNames.JMX_SESSION + "{path}/",
                new JmxMetric[] { ACTIVE_SESSIONS, EXPIRED_SESSIONS, REJECTED_SESSIONS, SESSION_ALIVE_TIME }));
        /*
         * Provides information about the thread pool. The current thread count and the current number of threads which
         * are busy.
         */
        metrics.add(new BaseJmxValue(name + ":type=ThreadPool,name=*", MetricNames.JMX_THREAD_POOL + "{name}/",
                new JmxMetric[] { CURRENT_ACTIVE_COUNT, CURRENT_IDLE_COUNT, CURRENT_MAX_COUNT }));
        
        /*
         * Provides information about the data source by finding the number of active and idle connections and the max connections. 
         * In Tomcat 7, the number of max connections is represented by the maxActive attribute. In tomcat 8
         * this was changed to maxTotal, hence the two CONNECTIONS_MAX metrics.
         */
        metrics.add(new BaseJmxValue(name + ":type=DataSource,context=*,host=*," +
                "class=javax.sql.DataSource,name=*", MetricNames.JMX_DATASOURCES + "{name}/",
                new JmxMetric[] { CONNECTIONS_ACTIVE, CONNECTIONS_IDLE, CONNECTIONS_MAX, CONNECTIONS_MAX_TOMCAT_8 }));
        
        /*
         * Provides information about the data source when the customer is using JNDI GlobalNamingResources, which do 
         * not have a context or a host.
         */
        metrics.add(new BaseJmxValue(name + ":type=DataSource," +
                "class=javax.sql.DataSource,name=*", MetricNames.JMX_DATASOURCES + "{name}/",
                new JmxMetric[] { CONNECTIONS_ACTIVE, CONNECTIONS_IDLE, CONNECTIONS_MAX, CONNECTIONS_MAX_TOMCAT_8 }));
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