/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class SqlTraceServiceImpl extends AbstractService implements SqlTraceService, TransactionListener, HarvestListener {
    private static final SlowQueryListener NOP_SLOW_QUERY_LISTENER = new NopSlowQueryListener();

    private final ConcurrentMap<String, SlowQueryAggregator> slowQueryAggregators = new ConcurrentHashMap<>();
    private final SlowQueryAggregator defaultSlowQueryAggregator;
    private final String defaultAppName;

    public SqlTraceServiceImpl() {
        super(SqlTraceService.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        defaultSlowQueryAggregator = createSlowQueryAggregator();
    }

    private boolean isEnabled(AgentConfig agentConfig) {
        if (!agentConfig.getSqlTraceConfig().isEnabled()) {
            return false;
        }
        TransactionTracerConfig ttConfig = agentConfig.getTransactionTracerConfig();
        if (SqlObfuscator.OFF_SETTING.equals(ttConfig.getRecordSql())) {
            return false;
        }
        if (ttConfig.isLogSql()) {
            return false;
        }
        return ttConfig.isEnabled();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    @Override
    protected void doStop() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData td, TransactionStats transactionStats) {
        SlowQueryAggregator aggregator = getOrCreateSlowQueryAggregator(td.getApplicationName());
        aggregator.addSlowQueriesFromTransaction(td);
    }

    @Override
    public SlowQueryListener getSlowQueryListener(String appName) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(appName);
        if (isEnabled(agentConfig)) {
            double threshold = agentConfig.getTransactionTracerConfig().getExplainThresholdInMillis();
            return new DefaultSlowQueryListener(appName, threshold);
        }
        return NOP_SLOW_QUERY_LISTENER;
    }

    @Override
    public void afterHarvest(String appName) {
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        try {
            SlowQueryAggregator aggregator = getOrCreateSlowQueryAggregator(appName);
            List<SqlTrace> sqlTraces = aggregator.getAndClearSlowQueries();
            try {
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(appName)
                        .sendSqlTraceData(sqlTraces);
            } catch (Exception e) {
                // HttpError/LicenseException handled here
                String msg = MessageFormat.format("Error sending sql traces for {0}: {1}", appName, e);
                if (getLogger().isLoggable(Level.FINEST)) {
                    getLogger().log(Level.FINEST, msg, e);
                } else {
                    getLogger().fine(msg);
                }
            }
        } catch (Throwable thr) {
            if (getLogger().isLoggable(Level.FINEST)) {
                getLogger().log(Level.FINEST, thr, "Error grabbing sql tracers during harvest for app {0}", appName);
            } else {
                getLogger().log(Level.FINE, "Error grabbing sql tracers during harvest for app {0}. {1}", appName, thr.getMessage());
            }
        }
    }

    private SlowQueryAggregator getOrCreateSlowQueryAggregator(String appName) {
        SlowQueryAggregator slowQueryAggregator = getSlowQueryAggregator(appName);
        if (slowQueryAggregator != null) {
            return slowQueryAggregator;
        }
        slowQueryAggregator = createSlowQueryAggregator();
        SlowQueryAggregator oldSlowQueryAggregator = slowQueryAggregators.putIfAbsent(appName, slowQueryAggregator);
        return oldSlowQueryAggregator == null ? slowQueryAggregator : oldSlowQueryAggregator;
    }

    private SlowQueryAggregator getSlowQueryAggregator(String appName) {
        if (appName == null || appName.equals(defaultAppName)) {
            return defaultSlowQueryAggregator;
        }
        return slowQueryAggregators.get(appName);
    }

    private SlowQueryAggregator createSlowQueryAggregator() {
        return new SlowQueryAggregatorImpl();
    }

}
