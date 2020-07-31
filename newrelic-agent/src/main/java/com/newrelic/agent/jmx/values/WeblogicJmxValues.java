/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.create.JmxAttributeFilter;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.DataSourceJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.EjbPoolJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.EjbTransactionJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.JMXMetricType;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.JtaJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class WeblogicJmxValues extends JmxFrameworkValues {

    public static final String PREFIX = "com.bea";

    private static final int METRIC_COUNT = 1;
    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);
    private static final JmxAttributeFilter FILTER = new JmxAttributeFilter() {

        @Override
        public boolean keepMetric(String rootMetricName) {
            if (rootMetricName.contains("/uuid-")) {
                // if it is an instance then do not sent it up
                Agent.LOG.log(Level.FINER,
                        "Weblogic JMX metric {0} is being ignored because it appears to be an instance.",
                        rootMetricName);
                return false;
            }
            return true;
        }
    };

    static {

        /*
         * Provides information on thread pools. ExecuteThreadIdleCount is the number of idle threads in the pool.
         * Through put is the number of requests per second. StandbyThreadCount is the number of threads in the standby
         * pool (threads not needed to handle present work). HoggingThreadCount is the threads that are being held by a
         * request right now.
         */
        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=ThreadPoolRuntime,Type=ThreadPoolRuntime",
                MetricNames.JMX_THREAD_POOL + "{Name}/", new JmxMetric[] {
                        ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("HoggingThreadCount"),
                        ServerJmxMetricGenerator.IDLE_THREAD_POOL_COUNT.createMetric("ExecuteThreadIdleCount"),
                        ServerJmxMetricGenerator.STANDBY_THREAD_POOL_COUNT.createMetric("StandbyThreadCount") }));

        METRICS.add(new BaseJmxValue(
                "com.bea:ServerRuntime=*,Name=*,Type=JDBCDataSourceRuntime",
                MetricNames.JMX_DATASOURCES + "{Name}/",
                new JmxMetric[] {
                        DataSourceJmxMetricGenerator.CONNECTIONS_AVAILABLE.createMetric("NumAvailable"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_POOL_SIZE.createMetric("CurrCapacity"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_CREATED.createMetric("ConnectionsTotalCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE.createMetric("ActiveConnectionsCurrentCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_LEAKED.createMetric("LeakedConnectionCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_CACHE_SIZE.createMetric("PrepStmtCacheCurrentSize"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_WAITING_COUNT.createMetric("WaitingForConnectionCurrentCount"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_TOTAL_COUNT.createMetric("WaitingForConnectionTotal"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_SUCCESS.createMetric("WaitingForConnectionSuccessTotal"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_FAILURE.createMetric("WaitingForConnectionFailureTotal") }));

        METRICS.add(new BaseJmxValue(
                "com.bea:ServerRuntime=*,Name=*,Type=JDBCOracleDataSourceRuntime",
                MetricNames.JMX_DATASOURCES + "{Name}/",
                new JmxMetric[] {
                        DataSourceJmxMetricGenerator.CONNECTIONS_AVAILABLE.createMetric("NumAvailable"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_POOL_SIZE.createMetric("CurrCapacity"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_CREATED.createMetric("ConnectionsTotalCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_ACTIVE.createMetric("ActiveConnectionsCurrentCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_LEAKED.createMetric("LeakedConnectionCount"),
                        DataSourceJmxMetricGenerator.CONNECTIONS_CACHE_SIZE.createMetric("PrepStmtCacheCurrentSize"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_WAITING_COUNT.createMetric("WaitingForConnectionCurrentCount"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_TOTAL_COUNT.createMetric("WaitingForConnectionTotal"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_SUCCESS.createMetric("WaitingForConnectionSuccessTotal"),
                        DataSourceJmxMetricGenerator.CONNECTION_REQUEST_FAILURE.createMetric("WaitingForConnectionFailureTotal") }));

        // EJB Pool metrics - these are per bean
        METRICS.add(new BaseJmxValue(
                "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBPoolRuntime,EJBComponentRuntime=*,*",
                MetricNames.JMX_EJB_POOL + "{ApplicationRuntime}/{EJBComponentRuntime}/{Name}/", FILTER,
                new JmxMetric[] { EjbPoolJmxMetricGenerator.SUCCESS.createMetric("AccessTotalCount", "MissTotalCount"),
                        EjbPoolJmxMetricGenerator.FAILURE.createMetric("MissTotalCount"),
                        EjbPoolJmxMetricGenerator.THREADS_WAITING.createMetric("WaiterCurrentCount"),
                        EjbPoolJmxMetricGenerator.DESTROY.createMetric("DestroyedTotalCount"),
                        EjbPoolJmxMetricGenerator.ACTIVE.createMetric("BeansInUseCurrentCount"),
                        EjbPoolJmxMetricGenerator.AVAILABLE.createMetric("PooledBeansCurrentCount") }));

        // EJB Transaction metrics - per transaction
        // for the application
        METRICS.add(new BaseJmxValue(
                "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                MetricNames.JMX_EJB_TRANSACTION_APPLICATION + "{ApplicationRuntime}/", FILTER, null,
                JMXMetricType.SUM_ALL_BEANS, new JmxMetric[] {
                        EjbTransactionJmxMetricGenerator.COUNT.createMetric("TransactionsCommittedTotalCount",
                                "TransactionsRolledBackTotalCount", "TransactionsTimedOutTotalCount"),
                        EjbTransactionJmxMetricGenerator.COMMIT.createMetric("TransactionsCommittedTotalCount"),
                        EjbTransactionJmxMetricGenerator.ROLLBACK.createMetric("TransactionsRolledBackTotalCount"),
                        EjbTransactionJmxMetricGenerator.TIMEOUT.createMetric("TransactionsTimedOutTotalCount") }));

        // for the module
        METRICS.add(new BaseJmxValue(
                "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                MetricNames.JMX_EJB_TRANSACTION_MODULE + "{ApplicationRuntime}/{EJBComponentRuntime}/", FILTER, null,
                JMXMetricType.SUM_ALL_BEANS, new JmxMetric[] {
                        EjbTransactionJmxMetricGenerator.COUNT.createMetric("TransactionsCommittedTotalCount",
                                "TransactionsRolledBackTotalCount", "TransactionsTimedOutTotalCount"),
                        EjbTransactionJmxMetricGenerator.COMMIT.createMetric("TransactionsCommittedTotalCount"),
                        EjbTransactionJmxMetricGenerator.ROLLBACK.createMetric("TransactionsRolledBackTotalCount"),
                        EjbTransactionJmxMetricGenerator.TIMEOUT.createMetric("TransactionsTimedOutTotalCount") }));

        // bean level
        METRICS.add(new BaseJmxValue(
                "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                MetricNames.JMX_EJB_TRANSACTION_BEAN + "{ApplicationRuntime}/{EJBComponentRuntime}/{Name}/", FILTER,
                new JmxMetric[] {
                        EjbTransactionJmxMetricGenerator.COUNT.createMetric("TransactionsCommittedTotalCount",
                                "TransactionsRolledBackTotalCount", "TransactionsTimedOutTotalCount"),
                        EjbTransactionJmxMetricGenerator.COMMIT.createMetric("TransactionsCommittedTotalCount"),
                        EjbTransactionJmxMetricGenerator.ROLLBACK.createMetric("TransactionsRolledBackTotalCount"),
                        EjbTransactionJmxMetricGenerator.TIMEOUT.createMetric("TransactionsTimedOutTotalCount") }));

        METRICS.add(new BaseJmxValue("com.bea:ServerRuntime=*,Name=JTARuntime,Type=JTARuntime", MetricNames.JMX_JTA
                + "{Name}/", FILTER, new JmxMetric[] {
                JtaJmxMetricGenerator.COUNT.createMetric("TransactionTotalCount"),
                JtaJmxMetricGenerator.COMMIT.createMetric("TransactionCommittedTotalCount"),
                JtaJmxMetricGenerator.ROLLBACK.createMetric("TransactionRolledBackTotalCount"),
                JtaJmxMetricGenerator.ABANDONDED.createMetric("TransactionAbandonedTotalCount") }));
    }

    public WeblogicJmxValues() {
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
