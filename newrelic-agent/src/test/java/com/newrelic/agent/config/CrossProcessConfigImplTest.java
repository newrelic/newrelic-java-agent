/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Mocks;
import com.newrelic.agent.util.Obfuscator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrossProcessConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(CrossProcessConfigImpl.ENABLED, !CrossProcessConfigImpl.DEFAULT_ENABLED);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(localMap);

        Assert.assertEquals(!CrossProcessConfigImpl.DEFAULT_ENABLED, config.isCrossApplicationTracing());
    }

    @Test
    public void isEnabledDeprecated() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, !CrossProcessConfigImpl.DEFAULT_ENABLED);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(localMap);

        Assert.assertEquals(!CrossProcessConfigImpl.DEFAULT_ENABLED, config.isCrossApplicationTracing());
    }

    @Test
    public void isEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = CrossProcessConfigImpl.SYSTEM_PROPERTY_ROOT + CrossProcessConfigImpl.ENABLED;
        String val = String.valueOf(!CrossProcessConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(CrossProcessConfigImpl.ENABLED, CrossProcessConfigImpl.DEFAULT_ENABLED);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(localMap);

        Assert.assertEquals(!CrossProcessConfigImpl.DEFAULT_ENABLED, config.isCrossApplicationTracing());
    }

    @Test
    public void isEnabledDefault() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(localMap);

        Assert.assertEquals(CrossProcessConfigImpl.DEFAULT_ENABLED, config.isCrossApplicationTracing());
    }

    @Test
    public void trustedIds() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        List<String> trustedIds = new ArrayList<>();
        trustedIds.add("12345");
        trustedIds.add("12345");
        trustedIds.add("54321");
        ServerProp serverProp = ServerProp.createPropObject(trustedIds);
        serverSettings.put(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS, serverProp);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(serverSettings);

        Assert.assertTrue(config.isTrustedAccountId("12345"));
        Assert.assertTrue(config.isTrustedAccountId("54321"));
        Assert.assertFalse(config.isTrustedAccountId("12abc345"));
    }

    @Test
    public void encodingKey() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();

        // CAT must be enabled, or the encoding key will be null
        serverSettings.put(CrossProcessConfigImpl.ENABLED, true);

        String encodingKey = "test";
        ServerProp serverProp = ServerProp.createPropObject(encodingKey);
        serverSettings.put(CrossProcessConfigImpl.ENCODING_KEY, serverProp);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(serverSettings);

        Assert.assertEquals("test", config.getEncodingKey());
    }

    @Test
    public void crossApplicationTracing() {
        Map<String, Object> serverSettings = new HashMap<>();
        String encodingKey = "test";
        String crossProcessId = "1#23";
        boolean crossApplicationTracing = true;

        ServerProp serverProp = ServerProp.createPropObject(encodingKey);
        serverSettings.put(CrossProcessConfigImpl.ENCODING_KEY, serverProp);
        serverProp = ServerProp.createPropObject(crossProcessId);
        serverSettings.put(CrossProcessConfigImpl.CROSS_PROCESS_ID, serverProp);
        serverProp = ServerProp.createPropObject(crossApplicationTracing);
        serverSettings.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, serverProp);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(serverSettings);

        Assert.assertEquals("1#23", config.getCrossProcessId());
        Assert.assertEquals("test", config.getEncodingKey());
        Assert.assertEquals(Obfuscator.obfuscateNameUsingKey("test", crossProcessId), config.getEncodedCrossProcessId());
        Assert.assertEquals(crossApplicationTracing, config.isCrossApplicationTracing());
    }

    @Test
    public void crossApplicationTracingDisabled() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        String encodingKey = "test";
        String crossProcessId = "1#23";
        boolean crossApplicationTracing = false;

        ServerProp serverProp = ServerProp.createPropObject(encodingKey);
        serverSettings.put(CrossProcessConfigImpl.ENCODING_KEY, serverProp);
        serverProp = ServerProp.createPropObject(crossProcessId);
        serverSettings.put(CrossProcessConfigImpl.CROSS_PROCESS_ID, serverProp);
        serverProp = ServerProp.createPropObject(crossApplicationTracing);
        serverSettings.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, serverProp);
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(serverSettings);

        Assert.assertNull(config.getCrossProcessId());
        Assert.assertNull(config.getEncodingKey());
        Assert.assertNull(config.getEncodedCrossProcessId());
        Assert.assertEquals(crossApplicationTracing, config.isCrossApplicationTracing());
    }

    @Test
    public void localSettings() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        CrossProcessConfig config = CrossProcessConfigImpl.createCrossProcessConfig(localSettings);

        Assert.assertNull(config.getCrossProcessId());
        Assert.assertNull(config.getEncodingKey());
        Assert.assertNull(config.getEncodedCrossProcessId());
    }

}
