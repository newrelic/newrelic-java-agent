/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.ConnectionConfigListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigServiceTest {

    @After
    public void afterTest() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    private void createServiceManager(Map<String, Object> configMap) throws Exception {
        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(configMap);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        configService.start();
        serviceManager.start();

    }

    @Test
    public void constructedWithoutStuff() throws Exception {
        AgentConfig emptyConfig = AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(),
                Collections.<String, Object>emptyMap(),
                Collections.<String, Boolean>emptyMap());

        File noConfigFile = null;
        Map<String, Object> noFileMeansNoSettings = null;
        ConfigService target = new ConfigServiceImpl(emptyConfig, noConfigFile, noFileMeansNoSettings, false);

        target.start();
        target.stop();
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(configMap);

        ConfigService configService = ServiceFactory.getConfigService();
        assertTrue(configService.isEnabled());
    }

    @Test
    public void connectionListener() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(configMap);

        ConfigService configService = ServiceFactory.getConfigService();
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        ConnectionConfigListener connectionConfigListener = rpmServiceManager.getConnectionConfigListener();

        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        String appName = rpmService.getApplicationName();
        String appName2 = "bogus";
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> agentData = new HashMap<>();
        data.put(AgentConfigFactory.AGENT_CONFIG, agentData);
        data.put(AgentConfigImpl.APDEX_T, 0.500d);
        data.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        data.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        agentData.put(AgentConfigFactory.TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.ENABLED, true);
        agentData.put(AgentConfigFactory.ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.ENABLED, true);

        assertFalse(configService.getAgentConfig(appName).getTransactionTracerConfig().isEnabled());
        assertFalse(configService.getAgentConfig(appName2).getTransactionTracerConfig().isEnabled());
        assertTrue(configService.getAgentConfig(appName).getErrorCollectorConfig().isEnabled());
        assertTrue(configService.getAgentConfig(appName2).getErrorCollectorConfig().isEnabled());

        connectionConfigListener.connected(rpmService, data);

        assertEquals(500L, configService.getAgentConfig(appName).getApdexTInMillis());
        assertEquals(1000L, configService.getAgentConfig(appName2).getApdexTInMillis());
        assertTrue(configService.getAgentConfig(appName).getTransactionTracerConfig().isEnabled());
        assertFalse(configService.getAgentConfig(appName2).getTransactionTracerConfig().isEnabled());
        assertTrue(configService.getAgentConfig(appName).getErrorCollectorConfig().isEnabled());
        assertTrue(configService.getAgentConfig(appName2).getErrorCollectorConfig().isEnabled());

        data.put(AgentConfigImpl.APDEX_T, 1.500d);

        connectionConfigListener.connected(rpmService, data);

        assertEquals(1500L, configService.getAgentConfig(appName).getApdexTInMillis());
        assertEquals(1000L, configService.getAgentConfig(appName2).getApdexTInMillis());

        rpmService = new MockRPMService();
        rpmService.setApplicationName(appName2);
        data.put(AgentConfigImpl.APDEX_T, 2.000d);
        connectionConfigListener.connected(rpmService, data);

        assertEquals(1500L, configService.getAgentConfig(appName).getApdexTInMillis());
        assertEquals(2000L, configService.getAgentConfig(appName2).getApdexTInMillis());
    }

    @Test
    public void connectionListenerAndErrorEvents() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(configMap);

        ConfigService configService = ServiceFactory.getConfigService();
        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        ConnectionConfigListener connectionConfigListener = rpmServiceManager.getConnectionConfigListener();

        // test defaults
        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        String appName = rpmService.getApplicationName();
        String appName2 = "bogus";
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> agentData = new HashMap<>();
        data.put(AgentConfigFactory.AGENT_CONFIG, agentData);
        assertTrue(configService.getAgentConfig(appName).getErrorCollectorConfig().isEventsEnabled());
        assertTrue(configService.getAgentConfig(appName2).getErrorCollectorConfig().isEventsEnabled());
        assertEquals(100, configService.getAgentConfig(appName).getErrorCollectorConfig().getMaxSamplesStored());
        assertEquals(100, configService.getAgentConfig(appName2).getErrorCollectorConfig().getMaxSamplesStored());

        // test collector shut off
        data.put(ErrorCollectorConfigImpl.COLLECT_EVENTS, false);
        connectionConfigListener.connected(rpmService, data);
        assertFalse(configService.getAgentConfig(appName).getErrorCollectorConfig().isEventsEnabled());
        assertTrue(configService.getAgentConfig(appName2).getErrorCollectorConfig().isEventsEnabled());
        assertEquals(100, configService.getAgentConfig(appName).getErrorCollectorConfig().getMaxSamplesStored());
        assertEquals(100, configService.getAgentConfig(appName2).getErrorCollectorConfig().getMaxSamplesStored());

        // test config shut off and max event count
        rpmService = new MockRPMService();
        rpmService.setApplicationName(appName2);
        agentData.put(AgentConfigFactory.CAPTURE_ERROR_EVENTS, false);
        agentData.put(AgentConfigFactory.MAX_ERROR_EVENT_SAMPLES_STORED, 20);
        connectionConfigListener.connected(rpmService, data);
        assertFalse(configService.getAgentConfig(appName).getErrorCollectorConfig().isEventsEnabled());
        assertFalse(configService.getAgentConfig(appName2).getErrorCollectorConfig().isEventsEnabled());
        assertEquals(100, configService.getAgentConfig(appName).getErrorCollectorConfig().getMaxSamplesStored());
        assertEquals(20, configService.getAgentConfig(appName2).getErrorCollectorConfig().getMaxSamplesStored());
    }

    @Test
    public void apdexTInMillis() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(configMap);

        ConfigService configService = ServiceFactory.getConfigService();
        assertEquals(1000L, configService.getAgentConfig(null).getApdexTInMillis());
        String appName = configService.getDefaultAgentConfig().getApplicationName();
        assertEquals(1000L, configService.getAgentConfig(appName).getApdexTInMillis());

        assertEquals(1000L, configService.getAgentConfig("bogus").getApdexTInMillis());
    }

    @Test
    public void badServerData() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(configMap);

        MockRPMServiceManager rpmServiceManager = (MockRPMServiceManager) ServiceFactory.getRPMServiceManager();
        ConnectionConfigListener connectionConfigListener = rpmServiceManager.getConnectionConfigListener();

        MockRPMService rpmService = (MockRPMService) rpmServiceManager.getRPMService();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> agentData = new HashMap<>();
        data.put(AgentConfigFactory.AGENT_CONFIG, agentData);
        data.put(AgentConfigImpl.APDEX_T, 0.5d);
        data.put(AgentConfigFactory.COLLECT_TRACES, true);
        agentData.put(AgentConfigFactory.TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.ENABLED, "bad");
        agentData.put(AgentConfigFactory.ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.ENABLED,
                !ErrorCollectorConfigImpl.DEFAULT_ENABLED);

        connectionConfigListener.connected(rpmService, data);
        ConfigService configService = ServiceFactory.getConfigService();

        assertFalse(configService.getAgentConfig(null).getTransactionTracerConfig().isEnabled());
        assertEquals(ErrorCollectorConfigImpl.DEFAULT_ENABLED,
                configService.getAgentConfig(null).getErrorCollectorConfig().isEnabled());
    }

    @Test
    public void sanitizedSettings() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        configMap.put(AgentConfigImpl.PROXY_USER, "secret_user");
        configMap.put(AgentConfigImpl.PROXY_PASS, "secret_pass");
        configMap.put(AgentConfigImpl.PROXY_HOST, "secret_host");
        createServiceManager(configMap);

        ConfigService configService = ServiceFactory.getServiceManager().getConfigService();
        Map<String, Object> sanitizedSettings = configService.getSanitizedLocalSettings();
        assertEquals(sanitizedSettings.get(AgentConfigImpl.PROXY_USER), "****");
        assertEquals(sanitizedSettings.get(AgentConfigImpl.PROXY_PASS), "****");
        assertEquals(sanitizedSettings.get(AgentConfigImpl.PROXY_HOST), "****");
    }

    @Test
    public void sanitizerShouldAddAdaptiveSamplingTargetIfNotSet() throws Exception{
        //First, check if distributed_tracing.sampler.adaptive_sampling_target is added when nothing is yet there.
        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(settings);
        ConfigService configService = ServiceFactory.getConfigService();
        Map<String, Object> sanitizedSettings = configService.getSanitizedLocalSettings();

        Map<String, Object> dtSettings = (Map<String, Object>) sanitizedSettings.get("distributed_tracing");
        Map<String, Object> samplerSettings = (Map<String, Object>) dtSettings.get("sampler");
        assertEquals(SamplerConfig.DEFAULT_ADAPTIVE_SAMPLING_TARGET, samplerSettings.get(SamplerConfig.ADAPTIVE_SAMPLING_TARGET));

        //Second, put something in the dt config
        Map<String, Object> specifyDtSettings = new HashMap<>();
        settings.put("distributed_tracing", specifyDtSettings);
        specifyDtSettings.put("enabled", true);
        createServiceManager(settings);
        configService = ServiceFactory.getConfigService();
        sanitizedSettings = configService.getSanitizedLocalSettings();

        dtSettings = (Map<String, Object>) sanitizedSettings.get("distributed_tracing");
        assertEquals(true, dtSettings.get("enabled"));
        samplerSettings = (Map<String, Object>) dtSettings.get("sampler");
        assertEquals(SamplerConfig.DEFAULT_ADAPTIVE_SAMPLING_TARGET, samplerSettings.get(SamplerConfig.ADAPTIVE_SAMPLING_TARGET));

        //Third, put something in the sampler config (not adaptive_sampling_target)
        Map<String, Object> specifySamplerSettings = new HashMap<>();
        specifyDtSettings.put("sampler", specifySamplerSettings);
        specifySamplerSettings.put("remoteParentSampled", "always_on");
        createServiceManager(settings);
        configService = ServiceFactory.getConfigService();
        sanitizedSettings = configService.getSanitizedLocalSettings();

        //assert it all looks good, all the originally specified values are there.
        dtSettings = (Map<String, Object>) sanitizedSettings.get("distributed_tracing");
        assertEquals(true, dtSettings.get("enabled"));
        samplerSettings = (Map<String, Object>) dtSettings.get("sampler");
        assertEquals("always_on", samplerSettings.get("remoteParentSampled"));
        assertEquals(SamplerConfig.DEFAULT_ADAPTIVE_SAMPLING_TARGET, samplerSettings.get(SamplerConfig.ADAPTIVE_SAMPLING_TARGET));
    }

    @Test
    public void sanitizerShouldNotAlterAdaptiveSamplingTargetIfAlreadySet() throws Exception{
        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        Map<String, Object> specifyDtSettings = new HashMap<>();
        Map<String, Object> specifySamplerSettings = new HashMap<>();
        settings.put("distributed_tracing", specifyDtSettings);
        specifyDtSettings.put("enabled", true);
        specifyDtSettings.put("sampler", specifySamplerSettings);
        specifySamplerSettings.put("remoteParentSampled", "always_on");
        specifySamplerSettings.put("adaptive_sampling_target", 133);

        createServiceManager(settings);
        ConfigService configService = ServiceFactory.getConfigService();
        Map<String, Object> sanitizedSettings = configService.getSanitizedLocalSettings();

        //assert it all looks good, all the originally specified values are there.
        Map<String, Object> dtSettings = (Map<String, Object>) sanitizedSettings.get("distributed_tracing");
        assertEquals(true, dtSettings.get("enabled"));
        Map<String, Object> samplerSettings = (Map<String, Object>) dtSettings.get("sampler");
        assertEquals("always_on", samplerSettings.get("remoteParentSampled"));
        assertEquals(133, samplerSettings.get(SamplerConfig.ADAPTIVE_SAMPLING_TARGET));
    }

    @Test
    public void noUsernamePasswordProxy() throws Exception {
        Map<String, Object> configMap = AgentConfigFactoryTest.createStagingMap();
        createServiceManager(configMap);
        ConfigService configService = ServiceFactory.getServiceManager().getConfigService();
        Assert.assertNull(configService.getDefaultAgentConfig().getProxyHost());
        Assert.assertNull(configService.getDefaultAgentConfig().getProxyPassword());
    }

    @Test
    public void fileChangeAndThenConnectDoesActuallyChangeConfig() throws IOException {
        ServiceManager mockServiceManager = mock(ServiceManager.class);
        ClassTransformerService mockClassTransformerService = mock(ClassTransformerService.class);
        when(mockServiceManager.getClassTransformerService()).thenReturn(mockClassTransformerService);
        ServiceFactory.setServiceManager(mockServiceManager);

        String appName = "Unit Test";

        Map<String, Object> originalMap = ImmutableMap.<String, Object>of("app_name", appName);
        File mockConfigFile = File.createTempFile("ConfigServiceTest", null);
        try (OutputStream os = new FileOutputStream(mockConfigFile)) {
            os.write(JSONObject.toJSONString(Collections.singletonMap("common", originalMap)).getBytes());
        }
        assertTrue(mockConfigFile.setLastModified(15L));

        AgentConfig originalConfig = AgentConfigImpl.createAgentConfig(originalMap);
        final Boolean[] circuitBreakerSetting = new Boolean[] { null };
        assertTrue("Default circuitbreaker was expected to be true; it was apparently not.", originalConfig.getCircuitBreakerConfig().isEnabled());

        ConfigServiceImpl target = new ConfigServiceImpl(originalConfig, mockConfigFile, originalMap, false);
        target.addIAgentConfigListener(new AgentConfigListener() {
            @Override
            public void configChanged(String appName, AgentConfig agentConfig) {
                circuitBreakerSetting[0] = agentConfig.getCircuitBreakerConfig().isEnabled();
            }
        });

        // step 1: modify the file.
        try (OutputStream os = new FileOutputStream(mockConfigFile)) {
            os.write(JSONObject.toJSONString(Collections.singletonMap("common", ImmutableMap.of(
                    "app_name", appName,
                    "circuitbreaker", Collections.singletonMap("enabled", false)))).getBytes());
        }
        assertTrue("unable to set the last modified time on the mock config file.", mockConfigFile.setLastModified(System.currentTimeMillis()));
        target.afterHarvest(appName);

        assertNotNull("circuitbreaker setting should have been set; it was not", circuitBreakerSetting[0]);
        assertFalse("circuitbreaker setting has not changed from true to false; it should have!", circuitBreakerSetting[0]);
        circuitBreakerSetting[0] = null;

        // step 2: trigger connect.
        IRPMService mockRPMService = mock(IRPMService.class);
        when(mockRPMService.getApplicationName()).thenReturn(appName);
        target.connected(mockRPMService, Collections.<String, Object>emptyMap());

        // this should not have reverted to the original contents.
        assertNotNull("circuitbreaker setting should have been set; it was not", circuitBreakerSetting[0]);
        assertFalse("circuitbreaker setting has changed from false; it should not have!", circuitBreakerSetting[0]);
    }

    @Test
    public void shouldDeobfuscateLicenseKey() throws Exception {
        Map<String, Object> obscuringKeyConfigProps = new HashMap<>();
        obscuringKeyConfigProps.put("obscuring_key", "abc123");
        ObscuredYamlPropertyWrapper obfuscatedLicenseKey =
                new ObscuredYamlPropertyWrapper("NBFTAEprV1VbCFNRAgYGVwICU1FXBAQEWVsCU1FXBARTAAAAVVdVBg==");

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(ObscuringConfig.OBSCURING_CONFIG, obscuringKeyConfigProps);
        configMap.put(AgentConfigImpl.LICENSE_KEY, obfuscatedLicenseKey);
        configMap.put(AgentConfigImpl.APP_NAME, "Test");

        createServiceManager(configMap);
        ConfigService configService = ServiceFactory.getConfigService();

        AgentConfig config = configService.getAgentConfig("Test");

        String expectedDeobfuscatedKey = "Us01xX6789abcdef0123456789abcdef01234567";
        assertEquals(expectedDeobfuscatedKey, config.getLicenseKey());
    }

}