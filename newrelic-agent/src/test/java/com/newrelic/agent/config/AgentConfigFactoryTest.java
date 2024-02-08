/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.transport.CollectorMethods;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentConfigFactoryTest {

    public static Map<String, Object> createStagingMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "nope.example.invalid");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, "Test");
        return configMap;
    }

    public static Map<String, Object> createMap() {
        return new HashMap<>();
    }

    @Test
    public void noApdexT() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> serverData = createMap();

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
        Assert.assertEquals((long) AgentConfigImpl.DEFAULT_APDEX_T * 1000, agentConfig.getApdexTInMillis());
    }

    @Test
    public void noAgentData() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> serverData = createMap();
        serverData.put(AgentConfigImpl.APDEX_T, 2.0d);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
        Assert.assertEquals(2000L, agentConfig.getApdexTInMillis());
    }

    @Test
    public void agentDataLocalSettings() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> errorMap = createMap();
        localSettings.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        errorMap.put(ErrorCollectorConfigImpl.IGNORE_STATUS_CODES, "402, 403");
        Map<String, Object> serverData = createMap();
        serverData.put(AgentConfigImpl.APDEX_T, 5.0d);
        Map<String, Object> agentData = createMap();
        String key = AgentConfigFactory.ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.ENABLED;
        agentData.put(key, true);
        key = AgentConfigFactory.THREAD_PROFILER_PREFIX + ThreadProfilerConfigImpl.ENABLED;
        agentData.put(key, !ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        key = AgentConfigFactory.TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.ENABLED;
        agentData.put(key, true);
        serverData.put(AgentConfigFactory.AGENT_CONFIG, agentData);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
        Assert.assertTrue(agentConfig.getErrorCollectorConfig().isEnabled());
        Assert.assertEquals(2, agentConfig.getErrorCollectorConfig().getIgnoreStatusCodes().size());
        Assert.assertTrue(agentConfig.getErrorCollectorConfig().getIgnoreStatusCodes().containsAll(
                Arrays.asList(402, 403)));
        Assert.assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED,
                agentConfig.getThreadProfilerConfig().isEnabled());
        Assert.assertFalse(agentConfig.getTransactionTracerConfig().isEnabled());

        agentConfig = AgentConfigFactory.createAgentConfig(localSettings, null, null);
        Assert.assertEquals(ErrorCollectorConfigImpl.DEFAULT_ENABLED, agentConfig.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void agentDataNoLocalSettings() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> serverData = createMap();
        serverData.put(AgentConfigImpl.APDEX_T, 5.0d);
        Map<String, Object> agentData = createMap();
        String key = AgentConfigFactory.ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.ENABLED;
        agentData.put(key, !ErrorCollectorConfigImpl.DEFAULT_ENABLED);
        key = AgentConfigFactory.THREAD_PROFILER_PREFIX + ThreadProfilerConfigImpl.ENABLED;
        agentData.put(key, !ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        key = AgentConfigFactory.TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.ENABLED;
        agentData.put(key, !TransactionTracerConfigImpl.DEFAULT_ENABLED);
        serverData.put(AgentConfigFactory.AGENT_CONFIG, agentData);
        serverData.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
        Assert.assertEquals(!ErrorCollectorConfigImpl.DEFAULT_ENABLED,
                agentConfig.getErrorCollectorConfig().isEnabled());
        Assert.assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED,
                agentConfig.getThreadProfilerConfig().isEnabled());
        Assert.assertEquals(!TransactionTracerConfigImpl.DEFAULT_ENABLED,
                agentConfig.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void agentDataNoServerReinstrument() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> serverData = createMap();
        Map<String, Object> reinstrument = createMap();
        Map<String, Object> agentData = createMap();
        reinstrument.put("attributes_enabled", true);
        agentData.put("reinstrument", reinstrument);
        serverData.put(AgentConfigFactory.AGENT_CONFIG, agentData);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
        Assert.assertFalse(agentConfig.getReinstrumentConfig().isAttributesEnabled());
    }

    @Test
    public void collectErrors() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> errorMap = createMap();
        localSettings.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, true);
        Map<String, Object> serverSettings = createMap();
        serverSettings.put(AgentConfigImpl.APDEX_T, 5.0d);
        serverSettings.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, false);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null);
        Assert.assertFalse(agentConfig.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void collectErrorsNoServerData() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> errorMap = createMap();
        localSettings.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, true);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, null, null);
        Assert.assertTrue(agentConfig.getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void collectTraces() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> ttSettings = createMap();
        localSettings.put(AgentConfigImpl.TRANSACTION_TRACER, ttSettings);
        ttSettings.put(TransactionTracerConfigImpl.ENABLED, true);
        Map<String, Object> serverSettings = createMap();
        serverSettings.put(AgentConfigImpl.APDEX_T, 5.0d);
        serverSettings.put(TransactionTracerConfigImpl.COLLECT_TRACES, false);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null);
        Assert.assertFalse(agentConfig.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void collectTracesNoServerData() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> ttSettings = createMap();
        localSettings.put(AgentConfigImpl.TRANSACTION_TRACER, ttSettings);
        ttSettings.put(TransactionTracerConfigImpl.ENABLED, true);

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, null, null);
        Assert.assertFalse(agentConfig.getTransactionTracerConfig().isEnabled());
    }

    @Test
    public void serverSideOverride() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> ttSettings = createMap();
        localSettings.put(AgentConfigImpl.TRANSACTION_TRACER, ttSettings);
        ttSettings.put(TransactionTracerConfigImpl.RECORD_SQL, SqlObfuscator.RAW_SETTING);
        Map<String, Object> serverSettings = createMap();
        serverSettings.put(AgentConfigImpl.APDEX_T, 5.0d);
        Map<String, Object> agentConfig = createMap();
        agentConfig.put(AgentConfigFactory.RECORD_SQL, SqlObfuscator.OBFUSCATED_SETTING);
        serverSettings.put(AgentConfigFactory.AGENT_CONFIG, agentConfig);
        AgentConfig config = AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null);
        Assert.assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
    }

    @Test
    public void crossProcess() throws Exception {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> catSettings = createMap();
        Map<String, Object> serverData = createMap();

        // cat must be enabled for the other properties to be returned
        catSettings.put(CrossProcessConfigImpl.ENABLED, true);
        localSettings.put(AgentConfigImpl.CROSS_APPLICATION_TRACER, catSettings);

        List<String> trustedIds = new ArrayList<>();
        trustedIds.add("12345");
        serverData.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, false); // deprecated setting is ignored when coming from the server
        serverData.put(CrossProcessConfigImpl.CROSS_PROCESS_ID, "1234#5678");
        serverData.put(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS, trustedIds);
        serverData.put(CrossProcessConfigImpl.ENCODING_KEY, "test");

        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
        Assert.assertTrue(agentConfig.getCrossProcessConfig().isCrossApplicationTracing());
        Assert.assertEquals("1234#5678", agentConfig.getCrossProcessConfig().getCrossProcessId());
        Assert.assertEquals("test", agentConfig.getCrossProcessConfig().getEncodingKey());
        Assert.assertTrue(agentConfig.getCrossProcessConfig().isTrustedAccountId("12345"));
    }

    @Test
    public void testAddSimpleMappedProperty() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        AgentConfigFactory.addSimpleMappedProperty("one.zee", "lemons", settings);

        Map<String, Object> expected = new HashMap<>();
        expected.put("zee", "lemons");
        Assert.assertEquals(expected, settings.get("one"));
    }

    @Test
    public void loggingDisabledServerSideOverride() {
        Map<String, Object> localSettings = logForwardingSettingsEnabled(true);
        Map<String, Object> serverSettings = loggingHarvestLimit(0);
        AgentConfig config = AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null);
        Assert.assertFalse(config.getApplicationLoggingConfig().isForwardingEnabled());
    }

    @Test
    public void loggingEnabledServerSideOverride() {
        Map<String, Object> localSettings = logForwardingSettingsEnabled(true);
        Map<String, Object> serverSettings = loggingHarvestLimit(10);
        AgentConfig config = AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null);
        Assert.assertTrue(config.getApplicationLoggingConfig().isForwardingEnabled());
    }

    @Test
    public void loggingDisabledLocally() {
        Map<String, Object> localSettings = logForwardingSettingsEnabled(false);
        Map<String, Object> serverSettings = loggingHarvestLimit(10);
        AgentConfig config = AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null);
        Assert.assertFalse(config.getApplicationLoggingConfig().isForwardingEnabled());
    }

    private Map<String, Object> logForwardingSettingsEnabled(boolean enabled) {
        Map<String, Object> localSettings = createMap();
        Map<String, Object> loggingSettings = createMap();
        Map<String, Object> forwardingSettings = createMap();

        localSettings.put(AgentConfigImpl.APPLICATION_LOGGING, loggingSettings);
        loggingSettings.put(ApplicationLoggingConfigImpl.FORWARDING, forwardingSettings);
        forwardingSettings.put(ApplicationLoggingForwardingConfig.ENABLED, enabled);
        return localSettings;
    }

    private Map<String, Object> loggingHarvestLimit(int limit) {
        Map<String, Object> serverSettings = createMap();
        Map<String, Object> harvestConfig = createMap();
        Map<String, Object> harvestLimits = createMap();
        serverSettings.put(AgentConfigFactory.EVENT_HARVEST_CONFIG, harvestConfig);
        harvestConfig.put(HarvestServiceImpl.HARVEST_LIMITS, harvestLimits);
        harvestLimits.put(CollectorMethods.LOG_EVENT_DATA, limit);
        return serverSettings;
    }
}
