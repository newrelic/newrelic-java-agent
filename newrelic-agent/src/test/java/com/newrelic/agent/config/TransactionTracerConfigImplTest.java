/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Mocks;
import com.newrelic.agent.database.SqlObfuscator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionTracerConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.ENABLED, !TransactionTracerConfigImpl.DEFAULT_ENABLED);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.ENABLED, !TransactionTracerConfigImpl.DEFAULT_ENABLED);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledServerNotCollectErrors() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.ENABLED, true);
        serverSettings.put(TransactionTracerConfigImpl.COLLECT_TRACES, false);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(false, config.isEnabled());
    }

    @Test
    public void isEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.ENABLED;
        String val = String.valueOf(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        serverSettings.put(TransactionTracerConfigImpl.ENABLED, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(false, config.isEnabled());
    }

    @Test
    public void isGCTimeEnabled() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.GC_TIME_ENABLED,
                !TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED, config.isGCTimeEnabled());
    }

    @Test
    public void isGCTimeEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.GC_TIME_ENABLED;
        String val = String.valueOf(!TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED, config.isGCTimeEnabled());
    }

    @Test
    public void isGCTimeEnabledServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.GC_TIME_ENABLED,
                !TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED, config.isGCTimeEnabled());
    }

    @Test
    public void isGCTimeEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.GC_TIME_ENABLED;
        String val = String.valueOf(!TransactionTracerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED);
        serverSettings.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED, config.isGCTimeEnabled());
    }

    @Test
    public void isGCTimeEnabledDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_GC_TIME_ENABLED, config.isGCTimeEnabled());
    }

    @Test
    public void isExplainEnabled() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.EXPLAIN_ENABLED,
                !TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED, config.isExplainEnabled());
    }

    @Test
    public void isExplainEnabledServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.EXPLAIN_ENABLED,
                !TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED, config.isExplainEnabled());
    }

    @Test
    public void isExplainEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.EXPLAIN_ENABLED;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED);
        serverSettings.put(TransactionTracerConfigImpl.EXPLAIN_ENABLED, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED, config.isExplainEnabled());
    }

    @Test
    public void isExplainEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.EXPLAIN_ENABLED;
        String val = String.valueOf(!TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED, config.isExplainEnabled());
    }

    @Test
    public void isExplainEnabledDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_ENABLED, config.isExplainEnabled());
    }

    @Test
    public void isLogSql() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.LOG_SQL, !TransactionTracerConfigImpl.DEFAULT_LOG_SQL);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_LOG_SQL, config.isLogSql());
    }

    @Test
    public void isLogSqlServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.LOG_SQL, !TransactionTracerConfigImpl.DEFAULT_LOG_SQL);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_LOG_SQL, config.isLogSql());
    }

    @Test
    public void isLogSqlServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.LOG_SQL;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_LOG_SQL);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!TransactionTracerConfigImpl.DEFAULT_LOG_SQL);
        serverSettings.put(TransactionTracerConfigImpl.LOG_SQL, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_LOG_SQL, config.isLogSql());
    }

    @Test
    public void isLogSqlSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.LOG_SQL;
        String val = String.valueOf(!TransactionTracerConfigImpl.DEFAULT_LOG_SQL);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_LOG_SQL, config.isLogSql());
    }

    @Test
    public void isLogSqlDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_LOG_SQL, config.isLogSql());
    }

    @Test
    public void recordSql() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.RAW_SETTING);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(SqlObfuscator.RAW_SETTING, config.getRecordSql());
    }

    @Test
    public void recordSqlIntern() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.RAW_SETTING);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertSame(SqlObfuscator.RAW_SETTING, config.getRecordSql());
        Assert.assertTrue(SqlObfuscator.RAW_SETTING == config.getRecordSql());
    }

    @Test
    public void recordSqlBoolean() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.RECORD_SQL, false);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(SqlObfuscator.OFF_SETTING, config.getRecordSql());
    }

    @Test
    public void recordSqlServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.RAW_SETTING);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(SqlObfuscator.RAW_SETTING, config.getRecordSql());
    }

    @Test
    public void recordSqlServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.RECORD_SQL;
        String val = SqlObfuscator.OBFUSCATED_SETTING;
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(SqlObfuscator.RAW_SETTING);
        serverSettings.put(TransactionTracerConfigImpl.RECORD_SQL, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(SqlObfuscator.RAW_SETTING, config.getRecordSql());
    }

    @Test
    public void recordSqlSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.RECORD_SQL;
        String val = SqlObfuscator.RAW_SETTING;
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(SqlObfuscator.RAW_SETTING, config.getRecordSql());
    }

    @Test
    public void recordSqlDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getRecordSql());
    }

    @Test
    public void slowQueryWhitelistIgnoredIfHighSecurityNotSet() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.SLOW_QUERY_WHITELIST, "slow,query,list");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertTrue(config.getCollectSlowQueriesFromModules().isEmpty());
    }

    @Test
    public void slowQueryWhitelistHighSecurity() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.SLOW_QUERY_WHITELIST, "slow,query,list");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                true);

        Set<String> collectSlowQueriesFromModules = config.getCollectSlowQueriesFromModules();
        Assert.assertNotNull(collectSlowQueriesFromModules);
        Assert.assertEquals(3, collectSlowQueriesFromModules.size());
        Assert.assertTrue(collectSlowQueriesFromModules.contains("slow"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("query"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("list"));
    }

    @Test
    public void collectSlowQueriesFromIgnoredIfHighSecurityNotSet() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, "collect,slow,queries,from");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertTrue(config.getCollectSlowQueriesFromModules().isEmpty());
    }

    @Test
    public void collectSlowQueriesFromHighSecurity() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, "collect,slow,queries,from");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                true);

        Set<String> collectSlowQueriesFromModules = config.getCollectSlowQueriesFromModules();
        Assert.assertNotNull(collectSlowQueriesFromModules);
        Assert.assertEquals(4, collectSlowQueriesFromModules.size());
        Assert.assertTrue(collectSlowQueriesFromModules.contains("collect"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("slow"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("queries"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("from"));
    }

    @Test
    public void collectSlowQueriesFromOverridesSlowQueriesWhitelist() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, "collect,slow,queries,from");
        localSettings.put(TransactionTracerConfigImpl.SLOW_QUERY_WHITELIST, "slow,query,list");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                true);

        Set<String> collectSlowQueriesFromModules = config.getCollectSlowQueriesFromModules();
        Assert.assertNotNull(collectSlowQueriesFromModules);
        Assert.assertEquals(4, collectSlowQueriesFromModules.size());
        Assert.assertTrue(collectSlowQueriesFromModules.contains("collect"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("slow"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("queries"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("from"));
    }

    @Test
    public void slowQueryDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertTrue(config.getCollectSlowQueriesFromModules().isEmpty());
    }

    @Test
    public void slowQueryDefaultHighSecurity() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                true);

        Assert.assertTrue(config.getCollectSlowQueriesFromModules().isEmpty());
    }

    @Test
    public void slowQuerySystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.SLOW_QUERY_WHITELIST;
        String val = "slow,query,list";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertTrue(config.getCollectSlowQueriesFromModules().isEmpty());
    }

    @Test
    public void slowQuerySystemPropertyHighSecurity() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.SLOW_QUERY_WHITELIST;
        String val = "slow,query,list";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                true);

        Set<String> collectSlowQueriesFromModules = config.getCollectSlowQueriesFromModules();
        Assert.assertNotNull(collectSlowQueriesFromModules);
        Assert.assertEquals(3, collectSlowQueriesFromModules.size());
        Assert.assertTrue(collectSlowQueriesFromModules.contains("slow"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("query"));
        Assert.assertTrue(collectSlowQueriesFromModules.contains("list"));
    }

    @Test
    public void explainThreshold() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD, 1.0d);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(1000.0d, config.getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void explainThresholdServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD, 1.0d);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(1000.0d, config.getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void explainThresholdServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.EXPLAIN_THRESHOLD;
        String val = "5.0";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(1.0d);
        serverSettings.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(1000.0d, config.getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void explainThresholdSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.EXPLAIN_THRESHOLD;
        String val = "5.0";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(5000.0d, config.getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void explainThresholdDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD * 1000,
                config.getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void stackTraceThreshold() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.STACK_TRACE_THRESHOLD, 1.0d);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(1000.0d, config.getStackTraceThresholdInMillis(), .0001);
    }

    @Test
    public void stackTraceThresholdServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.STACK_TRACE_THRESHOLD, 1.0d);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(1000.0d, config.getStackTraceThresholdInMillis(), .0001);
    }

    @Test
    public void stackTraceThresholdServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.STACK_TRACE_THRESHOLD;
        String val = "5.0";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(1.0d);
        serverSettings.put(TransactionTracerConfigImpl.STACK_TRACE_THRESHOLD, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(1000.0d, config.getStackTraceThresholdInMillis(), .0001);
    }

    @Test
    public void stackTraceThresholdSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.STACK_TRACE_THRESHOLD;
        String val = "5.0";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(5000.0d, config.getStackTraceThresholdInMillis(), .001);
    }

    @Test
    public void stackTraceThresholdDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_STACK_TRACE_THRESHOLD * 1000,
                config.getStackTraceThresholdInMillis(), .0001);
    }

    @Test
    public void transactionTraceThreshold() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 1.0d);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(1000L, config.getTransactionThresholdInMillis());
    }

    @Test
    public void transactionTraceThresholdServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 1.0d);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(1000L, config.getTransactionThresholdInMillis());
    }

    @Test
    public void transactionTraceThresholdServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.TRANSACTION_THRESHOLD;
        String val = "5.0";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(1.0d);
        serverSettings.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(1000L, config.getTransactionThresholdInMillis());
    }

    @Test
    public void transactionTraceThresholdSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.TRANSACTION_THRESHOLD;
        String val = "5.0";
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(5000L, config.getTransactionThresholdInMillis());
    }

    @Test
    public void transactionTraceThresholdAdpexF() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, TransactionTracerConfigImpl.APDEX_F);
        long apdexT = 1000L;
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                apdexT, false);

        Assert.assertEquals(apdexT * TransactionTracerConfigImpl.APDEX_F_MULTIPLE,
                config.getTransactionThresholdInMillis());
    }

    @Test
    public void transactionTraceThresholdDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        long apdexT = 1000L;
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                apdexT, false);

        Assert.assertEquals(apdexT * TransactionTracerConfigImpl.APDEX_F_MULTIPLE,
                config.getTransactionThresholdInMillis());
    }

    @Test
    public void maxStackTraces() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.MAX_STACK_TRACE,
                TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1, config.getMaxStackTraces());
    }

    @Test
    public void maxStackTracesServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.MAX_STACK_TRACE,
                TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1, config.getMaxStackTraces());
    }

    @Test
    public void maxStackTracesServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.MAX_STACK_TRACE;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 2);
        serverSettings.put(TransactionTracerConfigImpl.MAX_STACK_TRACE, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 2, config.getMaxStackTraces());
    }

    @Test
    public void maxStackTracesSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.MAX_STACK_TRACE;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE + 1, config.getMaxStackTraces());
    }

    @Test
    public void maxStackTracesDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        long apdexT = 1000L;
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                apdexT, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_STACK_TRACE, config.getMaxStackTraces());
    }

    @Test
    public void maxSegments() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.SEGMENT_LIMIT,
                TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1, config.getMaxSegments());
    }

    @Test
    public void maxSegmentsServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.SEGMENT_LIMIT,
                TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1, config.getMaxSegments());
    }

    @Test
    public void maxSegmentsServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.SEGMENT_LIMIT;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 2);
        serverSettings.put(TransactionTracerConfigImpl.SEGMENT_LIMIT, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 2, config.getMaxSegments());
    }

    @Test
    public void maxSegmentsSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.SEGMENT_LIMIT;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT + 1, config.getMaxSegments());
    }

    @Test
    public void maxSegmentsDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        long apdexT = 1000L;
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                apdexT, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_SEGMENT_LIMIT, config.getMaxSegments());
    }

    @Test
    public void maxExplainPlans() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.MAX_EXPLAIN_PLANS,
                TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1, config.getMaxExplainPlans());
    }

    @Test
    public void maxExplainPlansServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(TransactionTracerConfigImpl.MAX_EXPLAIN_PLANS,
                TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1, config.getMaxExplainPlans());
    }

    @Test
    public void maxExplainPlansServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.MAX_EXPLAIN_PLANS;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 2);
        serverSettings.put(TransactionTracerConfigImpl.MAX_EXPLAIN_PLANS, serverProp);
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(serverSettings,
                500L, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 2, config.getMaxExplainPlans());
    }

    @Test
    public void maxExplainPlansSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.MAX_EXPLAIN_PLANS;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS + 1, config.getMaxExplainPlans());
    }

    @Test
    public void maxExplainPlansDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        long apdexT = 1000L;
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                apdexT, false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_MAX_EXPLAIN_PLANS, config.getMaxExplainPlans());
    }

    @Test
    public void createRequestTransactionTracerConfig() throws Exception {
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        requestCategoryMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD,
                TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1);
        categorySet.add(requestCategoryMap);
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals((TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1) * 1000,
                config.createRequestTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .001);
    }

    @Test
    public void createRequestTransactionTracerConfigSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.EXPLAIN_THRESHOLD;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        requestCategoryMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD,
                TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 2);
        categorySet.add(requestCategoryMap);
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals((TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1) * 1000,
                config.createRequestTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void createRequestTransactionTracerConfigServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.EXPLAIN_THRESHOLD;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
        ServerProp serverProp = ServerProp.createPropObject(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 2);
        requestCategoryMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD, serverProp);
        categorySet.add(requestCategoryMap);
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals((TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 2) * 1000,
                config.createRequestTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .001);
    }

    @Test
    public void createRequestTransactionTracerConfigDefault() throws Exception {
        Map<String, Object> ttMap = new HashMap<>();
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD * 1000,
                config.createRequestTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .001);
    }

    @Test
    public void createBackgroundTransactionTracerConfig() throws Exception {
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME);
        requestCategoryMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD,
                TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1);
        categorySet.add(requestCategoryMap);
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals((TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1) * 1000,
                config.createBackgroundTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void createBackgroundTransactionTracerConfigSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.EXPLAIN_THRESHOLD;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME);
        requestCategoryMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD,
                TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 2);
        categorySet.add(requestCategoryMap);
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals((TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1) * 1000,
                config.createBackgroundTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void createBackgroundTransactionTracerConfigServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = TransactionTracerConfigImpl.CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.EXPLAIN_THRESHOLD;
        String val = String.valueOf(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 1);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> ttMap = new HashMap<>();
        Set<Map<String, Object>> categorySet = new HashSet<>();
        ttMap.put(TransactionTracerConfigImpl.CATEGORY, categorySet);
        Map<String, Object> requestCategoryMap = new HashMap<>();
        requestCategoryMap.put(TransactionTracerConfigImpl.CATEGORY_NAME,
                TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME);
        ServerProp serverProp = ServerProp.createPropObject(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 2);
        requestCategoryMap.put(TransactionTracerConfigImpl.EXPLAIN_THRESHOLD, serverProp);
        categorySet.add(requestCategoryMap);
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals((TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD + 2) * 1000,
                config.createBackgroundTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void createBackgroundTransactionTracerConfigDefault() throws Exception {
        Map<String, Object> ttMap = new HashMap<>();
        long apdexT = 500L;
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(ttMap, apdexT,
                false);

        Assert.assertEquals(TransactionTracerConfigImpl.DEFAULT_EXPLAIN_THRESHOLD * 1000,
                config.createBackgroundTransactionTracerConfig(apdexT, false).getExplainThresholdInMillis(), .0001);
    }

    @Test
    public void systemSystemPropertyInheritance() throws Exception {
        Map<String, String> properties = new HashMap<>();

        properties.put(TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.ENABLED,
                String.valueOf(false));

        properties.put(TransactionTracerConfigImpl.CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.ENABLED, String.valueOf(true));

        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);

        // specific config
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                500L, false);
        Assert.assertEquals(config.isEnabled(), false);

        // category specific config
        TransactionTracerConfig requestConfig = config.createRequestTransactionTracerConfig(500L, false);
        Assert.assertEquals(requestConfig.isEnabled(), true);

        // no catogory specific config, fall back to inherited root value
        TransactionTracerConfig backgroundConfig = config.createBackgroundTransactionTracerConfig(500L, false);
        Assert.assertEquals(backgroundConfig.isEnabled(), false);
    }

    @Test
    public void environmentVariableInheritance() throws Exception {
        Map<String, String> properties = new HashMap<>();
        Map<String, String> environment = new HashMap<>();

        environment.put(TransactionTracerConfigImpl.SYSTEM_PROPERTY_ROOT + TransactionTracerConfigImpl.ENABLED,
                String.valueOf(false));

        environment.put(TransactionTracerConfigImpl.CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT
                + TransactionTracerConfigImpl.ENABLED, String.valueOf(true));

        Mocks.createSystemPropertyProvider(properties, environment);
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);

        // specific environment
        TransactionTracerConfigImpl config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings,
                500L, false);
        Assert.assertEquals(config.isEnabled(), false);

        // category specific environment
        TransactionTracerConfig requestConfig = config.createRequestTransactionTracerConfig(500L, false);
        Assert.assertEquals(requestConfig.isEnabled(), true);

        // no catogory specific environment, fall back to inherited root value
        TransactionTracerConfig backgroundConfig = config.createBackgroundTransactionTracerConfig(500L, false);
        Assert.assertEquals(backgroundConfig.isEnabled(), false);
    }

    @Test
    public void recordSqlOff() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.RECORD_SQL, "off");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L, false);

        Assert.assertEquals(false, config.isExplainEnabled());
    }

    @Test
    public void recordSqlOffHighSecurityOn() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(TransactionTracerConfigImpl.RECORD_SQL, "off");
        TransactionTracerConfig config = TransactionTracerConfigImpl.createTransactionTracerConfig(localSettings, 500L, true);

        Assert.assertEquals(false, config.isExplainEnabled());
    }

}
