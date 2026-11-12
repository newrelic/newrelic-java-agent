/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.serverless;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServerlessApiImplTest {

    private ServerlessApiImpl serverlessApi;

    @Before
    public void setup() {
        ServiceFactory.setServiceManager(new MockServiceManager());
        serverlessApi = new ServerlessApiImpl();
    }

    private void setServerlessModeEnabled(boolean enabled) {
        MockServiceManager serviceManager = new MockServiceManager();
        Map<String, Object> serverlessModeSettings = new HashMap<>();
        serverlessModeSettings.put("enabled", enabled);
        Map<String, Object> settings = new HashMap<>();
        settings.put("serverless_mode", serverlessModeSettings);
        serviceManager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void setAndGetArn() {
        String arn = "arn:aws:lambda:us-east-1:123456789012:function:my-function";
        serverlessApi.setArn(arn);
        assertEquals(arn, serverlessApi.getArn());
    }

    @Test
    public void setAndGetFunctionVersion() {
        String version = "$LATEST";
        serverlessApi.setFunctionVersion(version);
        assertEquals(version, serverlessApi.getFunctionVersion());
    }

    @Test
    public void getArn_whenNotSet_returnsNull() {
        assertNull(serverlessApi.getArn());
    }

    @Test
    public void getFunctionVersion_whenNotSet_returnsNull() {
        assertNull(serverlessApi.getFunctionVersion());
    }

    @Test
    public void setArn_withNull_doesNotStore() {
        serverlessApi.setArn("initial-arn");
        serverlessApi.setArn(null);
        assertEquals("initial-arn", serverlessApi.getArn());
    }

    @Test
    public void setArn_withEmptyString_doesNotStore() {
        serverlessApi.setArn("initial-arn");
        serverlessApi.setArn("");
        assertEquals("initial-arn", serverlessApi.getArn());
    }

    @Test
    public void setFunctionVersion_withNull_doesNotStore() {
        serverlessApi.setFunctionVersion("initial-version");
        serverlessApi.setFunctionVersion(null);
        assertEquals("initial-version", serverlessApi.getFunctionVersion());
    }

    @Test
    public void setFunctionVersion_withEmptyString_doesNotStore() {
        serverlessApi.setFunctionVersion("initial-version");
        serverlessApi.setFunctionVersion("");
        assertEquals("initial-version", serverlessApi.getFunctionVersion());
    }

    @Test
    public void setArn_canOverwritePreviousValue() {
        serverlessApi.setArn("first-arn");
        serverlessApi.setArn("second-arn");
        assertEquals("second-arn", serverlessApi.getArn());
    }

    @Test
    public void setFunctionVersion_canOverwritePreviousValue() {
        serverlessApi.setFunctionVersion("v1");
        serverlessApi.setFunctionVersion("v2");
        assertEquals("v2", serverlessApi.getFunctionVersion());
    }

    @Test
    public void isServerlessModeEnabled_delegatesToServerlessConfig() {
        setServerlessModeEnabled(true);
        assertTrue(serverlessApi.isServerlessModeEnabled());

        setServerlessModeEnabled(false);
        assertFalse(serverlessApi.isServerlessModeEnabled());
    }

    @Test
    public void addAndRemoveMetricCollector_delegatesToServerlessService() {
        Object metricReader = new Object();
        serverlessApi.addMetricReader(metricReader, o -> { });
        assertTrue(ServiceFactory.getServiceManager().getServerlessService().otelMetricsRegistered());

        serverlessApi.removeMetricCollector(metricReader);
        assertFalse(ServiceFactory.getServiceManager().getServerlessService().otelMetricsRegistered());
    }

    @Test
    public void otelHarvest_delegatesToServerlessService() {
        setServerlessModeEnabled(true);
        serverlessApi = new ServerlessApiImpl();

        boolean result = serverlessApi.otelHarvest("base64Payload");

        assertTrue(result);
        assertEquals("base64Payload", ServiceFactory.getServiceManager().getServerlessService().otelMetricsPayload());
    }
}
