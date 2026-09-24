/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.serverless;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServerlessServiceImplTest {

    private ServerlessServiceImpl serverlessService;

    @Before
    public void setup() {
        serverlessService = new ServerlessServiceImpl();
    }

    private HarvestService setupServiceManager(boolean serverlessModeEnabled) {
        MockServiceManager serviceManager = new MockServiceManager();
        Map<String, Object> serverlessModeSettings = new HashMap<>();
        serverlessModeSettings.put("enabled", serverlessModeEnabled);
        Map<String, Object> settings = new HashMap<>();
        settings.put("serverless_mode", serverlessModeSettings);
        serviceManager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
        ServiceFactory.setServiceManager(serviceManager);
        return serviceManager.getHarvestService();
    }

    @Test
    public void setAndGetArn() {
        String arn = "arn:aws:lambda:us-east-1:123456789012:function:my-function";
        serverlessService.setArn(arn);
        assertEquals(arn, serverlessService.getArn());
    }

    @Test
    public void setAndGetFunctionVersion() {
        String version = "$LATEST";
        serverlessService.setFunctionVersion(version);
        assertEquals(version, serverlessService.getFunctionVersion());
    }

    @Test
    public void getArn_whenNotSet_returnsNull() {
        assertNull(serverlessService.getArn());
    }

    @Test
    public void getFunctionVersion_whenNotSet_returnsNull() {
        assertNull(serverlessService.getFunctionVersion());
    }

    @Test
    public void setArn_withNull_doesNotStore() {
        serverlessService.setArn("initial-arn");
        serverlessService.setArn(null);
        assertEquals("initial-arn", serverlessService.getArn());
    }

    @Test
    public void setArn_withEmptyString_doesNotStore() {
        serverlessService.setArn("initial-arn");
        serverlessService.setArn("");
        assertEquals("initial-arn", serverlessService.getArn());
    }

    @Test
    public void setFunctionVersion_withNull_doesNotStore() {
        serverlessService.setFunctionVersion("initial-version");
        serverlessService.setFunctionVersion(null);
        assertEquals("initial-version", serverlessService.getFunctionVersion());
    }

    @Test
    public void setFunctionVersion_withEmptyString_doesNotStore() {
        serverlessService.setFunctionVersion("initial-version");
        serverlessService.setFunctionVersion("");
        assertEquals("initial-version", serverlessService.getFunctionVersion());
    }

    @Test
    public void setArn_canOverwritePreviousValue() {
        serverlessService.setArn("first-arn");
        serverlessService.setArn("second-arn");
        assertEquals("second-arn", serverlessService.getArn());
    }

    @Test
    public void setFunctionVersion_canOverwritePreviousValue() {
        serverlessService.setFunctionVersion("v1");
        serverlessService.setFunctionVersion("v2");
        assertEquals("v2", serverlessService.getFunctionVersion());
    }

    @Test
    public void otelMetricsRegistered_whenNoneAdded_returnsFalse() {
        assertFalse(serverlessService.otelMetricsRegistered());
    }

    @Test
    public void otelMetricsRegistered_afterAddMetricReader_returnsTrue() {
        serverlessService.addMetricReader(new Object(), o -> { });
        assertTrue(serverlessService.otelMetricsRegistered());
    }

    @Test
    public void addMetricReader_withNullMetricReader_isNoOp() {
        serverlessService.addMetricReader(null, o -> { });
        assertFalse(serverlessService.otelMetricsRegistered());
    }

    @Test
    public void addMetricReader_withNullReader_isNoOp() {
        serverlessService.addMetricReader(new Object(), null);
        assertFalse(serverlessService.otelMetricsRegistered());
    }

    @Test
    public void removeMetricCollector_removesRegisteredCollector() {
        Object metricReader = new Object();
        serverlessService.addMetricReader(metricReader, o -> { });
        serverlessService.removeMetricCollector(metricReader);
        assertFalse(serverlessService.otelMetricsRegistered());
    }

    @Test
    public void removeMetricCollector_whenNotRegistered_isNoOp() {
        serverlessService.removeMetricCollector(new Object());
        assertFalse(serverlessService.otelMetricsRegistered());
    }

    @Test
    public void collectOtelMetrics_invokesEachRegisteredCollectorWithItsMetricReader() {
        Object metricReaderOne = new Object();
        Object metricReaderTwo = new Object();
        List<Object> observed = new ArrayList<>();

        serverlessService.addMetricReader(metricReaderOne, observed::add);
        serverlessService.addMetricReader(metricReaderTwo, observed::add);

        serverlessService.collectOtelMetrics();

        assertEquals(2, observed.size());
        assertTrue(observed.contains(metricReaderOne));
        assertTrue(observed.contains(metricReaderTwo));
    }

    @Test
    public void collectOtelMetrics_whenNoneRegistered_doesNothing() {
        serverlessService.collectOtelMetrics();
    }

    @Test
    public void otelMetricsPayload_whenNeverHarvested_returnsNull() {
        assertNull(serverlessService.otelMetricsPayload());
    }

    @Test
    public void otelHarvest_whenServerlessModeEnabled_storesPayloadAndTriggersHarvest() {
        HarvestService harvestService = setupServiceManager(true);

        boolean result = serverlessService.otelHarvest("base64Payload");

        assertTrue(result);
        assertEquals("base64Payload", serverlessService.otelMetricsPayload());
        Mockito.verify(harvestService).harvestNow();
    }

    @Test
    public void otelHarvest_whenServerlessModeDisabled_doesNotStorePayloadOrHarvest() {
        HarvestService harvestService = setupServiceManager(false);

        boolean result = serverlessService.otelHarvest("base64Payload");

        assertTrue(result);
        assertNull(serverlessService.otelMetricsPayload());
        Mockito.verify(harvestService, Mockito.never()).harvestNow();
    }

    @Test
    public void otelHarvest_whenHarvestNowThrows_returnsFalse() {
        HarvestService harvestService = setupServiceManager(true);
        Mockito.doThrow(new RuntimeException("boom")).when(harvestService).harvestNow();

        boolean result = serverlessService.otelHarvest("base64Payload");

        assertFalse(result);
    }
}
