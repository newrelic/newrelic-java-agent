/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.SqlTracer;

import java.sql.Connection;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class DatabaseService extends AbstractService implements AgentConfigListener {

    private static final SqlObfuscator DEFAULT_SQL_OBFUSCATOR = SqlObfuscator.getDefaultSqlObfuscator();

    private final ConcurrentMap<String, SqlObfuscator> sqlObfuscators = new ConcurrentHashMap<>();
    private final AtomicReference<SqlObfuscator> defaultSqlObfuscator = new AtomicReference<>();
    private final String defaultAppName;
    private final DatabaseStatementParser databaseStatementParser;

    public DatabaseService() {
        super(DatabaseService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        defaultAppName = config.getApplicationName();
        databaseStatementParser = new DefaultDatabaseStatementParser();
    }

    @Override
    protected void doStart() {
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    @Override
    protected void doStop() {
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Returns the default sql obfuscator (the one that actually obfuscates). This is normally NOT the correct one to
     * use (which is {@link #getSqlObfuscator(String)}), but is used in situations in which we want to obfuscate the sql
     * to get a key for aggregation purposes.
     * 
     */
    public SqlObfuscator getDefaultSqlObfuscator() {
        return DEFAULT_SQL_OBFUSCATOR;
    }

    public SqlObfuscator getSqlObfuscator(String appName) {
        SqlObfuscator sqlObfuscator = findSqlObfuscator(appName);
        if (sqlObfuscator != null) {
            return sqlObfuscator;
        }
        return createSqlObfuscator(appName);
    }

    private SqlObfuscator findSqlObfuscator(String appName) {
        if (appName == null || appName.equals(defaultAppName)) {
            return defaultSqlObfuscator.get();
        }
        return sqlObfuscators.get(appName);
    }

    private SqlObfuscator createSqlObfuscator(String appName) {
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getTransactionTracerConfig(appName);
        SqlObfuscator sqlObfuscator = createSqlObfuscator(ttConfig);
        if (appName == null || appName.equals(defaultAppName)) {
            if (defaultSqlObfuscator.getAndSet(sqlObfuscator) == null) {
                logConfig(appName, ttConfig);
            }
        } else {
            if (sqlObfuscators.put(appName, sqlObfuscator) == null) {
                logConfig(appName, ttConfig);
            }
        }
        return sqlObfuscator;
    }

    private SqlObfuscator createSqlObfuscator(TransactionTracerConfig ttConfig) {
        if (!ttConfig.isEnabled()) {
            return SqlObfuscator.getNoSqlObfuscator();
        }
        String recordSql = ttConfig.getRecordSql();
        if (SqlObfuscator.OFF_SETTING.equals(recordSql)) {
            return SqlObfuscator.getNoSqlObfuscator();
        }
        if (SqlObfuscator.RAW_SETTING.equals(recordSql)) {
            return SqlObfuscator.getNoObfuscationSqlObfuscator();
        }
        return SqlObfuscator.getDefaultSqlObfuscator();
    }

    private void logConfig(String appName, TransactionTracerConfig ttConfig) {
        if (ttConfig.isLogSql()) {
            String msg = MessageFormat.format("Agent is configured to log {0} SQL for {1}", ttConfig.getRecordSql(),
                    appName);
            Agent.LOG.fine(msg);
        } else {
            String msg = MessageFormat.format("Agent is configured to send {0} SQL to New Relic for {1}",
                    ttConfig.getRecordSql(), appName);
            Agent.LOG.fine(msg);
        }
        if (!isValidRecordSql(ttConfig.getRecordSql())) {
            String msg = MessageFormat.format("Unknown value \"{0}\" for record_sql", ttConfig.getRecordSql(), appName);
            Agent.LOG.warning(msg);
        }
    }

    private boolean isValidRecordSql(String recordSql) {
        return SqlObfuscator.RAW_SETTING.equals(recordSql) || SqlObfuscator.OFF_SETTING.equals(recordSql)
                || SqlObfuscator.OBFUSCATED_SETTING.equals(recordSql);
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        Agent.LOG.fine(
                MessageFormat.format("Database service received configuration change notification for {0}", appName));
        if (appName == null || appName.equals(defaultAppName)) {
            defaultSqlObfuscator.set(null);
        } else {
            sqlObfuscators.remove(appName);
        }

        // Force a reload of the SQL comment config settings cached by the JdbcHelper
        JdbcHelper.invalidateMetadataCommentConfig();
    }

    public void runExplainPlan(SqlTracer sqlTracer) {
        ExplainPlanExecutor explainExecutor = sqlTracer.getExplainPlanExecutor();
        ConnectionFactory connectionFactory = sqlTracer.getConnectionFactory();

        if (explainExecutor == null || connectionFactory == null) {
            Agent.LOG.finest("Unable to execute query for explain plan");
            return;
        }

        if (sqlTracer.hasExplainPlan()) {
            return;
        }

        runExplainPlan(explainExecutor, connectionFactory);
    }

    private void runExplainPlan(ExplainPlanExecutor explainExecutor, ConnectionFactory connectionFactory) {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            DatabaseVendor vendor = connectionFactory.getDatabaseVendor();
            explainExecutor.runExplainPlan(this, connection, vendor);
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred executing an explain plan: {0}", t);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, t);
            } else {
                Agent.LOG.fine(msg);
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINER, "Unable to close connection", e);
                }
            }
        }
    }

    public DatabaseStatementParser getDatabaseStatementParser() {
        return databaseStatementParser;
    }

}
