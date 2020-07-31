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

public class ResinJmxValues extends JmxFrameworkValues {

    public static final String PREFIX = "resin";

    private static final int METRIC_COUNT = 4;
    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    static {

        /*
         * Provides information about sessions. The SessionActiveCount is the current number of active sessions. The
         * SessionInvalidateCountTotal is the total number of invalidated sessions. SessionCreateCountTotal is the total
         * number of sessions created. SessionTimeoutCountTotal is the total number of sessions which timed out.
         */
        METRICS.add(new BaseJmxValue("resin:type=SessionManager,*", MetricNames.JMX_SESSION + "{WebApp}/",
                new JmxMetric[] { ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("SessionActiveCount"),
                        ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("SessionTimeoutCountTotal") }));

        /*
         * Datasource metrics
         */
        METRICS.add(new BaseJmxValue("resin:type=ConnectionPool,*",
                MetricNames.JMX_DATASOURCES + "{name}/", new JmxMetric[] {
                DataSourceJmxMetricGenerator.CONNECTIONS_POOL_SIZE.createMetric("ConnectionCount"),
                DataSourceJmxMetricGenerator.CONNECTIONS_IDLE.createMetric("ConnectionIdleCount"),
                DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE.createMetric("ConnectionActiveCount"),
                DataSourceJmxMetricGenerator.CONNECTIONS_CREATED.createMetric("ConnectionCreateCountTotal"),
                DataSourceJmxMetricGenerator.CONNECTIONS_MAX.createMetric("MaxConnections") }));

        /*
         * Provides information on thread pools.
         */
        METRICS.add(new BaseJmxValue("resin:type=ThreadPool", MetricNames.JMX_THREAD_POOL + "Resin/", new JmxMetric[] {
                ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("ThreadActiveCount"),
                ServerJmxMetricGenerator.IDLE_THREAD_POOL_COUNT.createMetric("ThreadIdleCount"),
                ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("ThreadMax") }));

        /*
         * This is in resin 4.0 pro only. CommitCountTotal is the total number of committed transaction.
         * CommitResourceFAilCountTotal is the total number of failed committed transactions. RollbackCountTotal is the
         * total number of rolledback transaction. TransactionCount is the count of in-progress transactions.
         */
        METRICS.add(new BaseJmxValue("resin:type=TransactionManager", MetricNames.JMX_TRANSACITON, new JmxMetric[] {
                ServerJmxMetricGenerator.TRANS_ROLLED_BACK_COUNT.createMetric("RollbackCountTotal"),
                ServerJmxMetricGenerator.TRANS_COMMITED_COUNT.createMetric("CommitCountTotal"),
                ServerJmxMetricGenerator.TRANS_ACTIVE_COUNT.createMetric("TransactionCount") }));

    }

    public ResinJmxValues() {
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
