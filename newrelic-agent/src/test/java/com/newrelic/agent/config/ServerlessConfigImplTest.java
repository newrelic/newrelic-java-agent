/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Mocks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerlessConfigImplTest {
    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabled() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ServerlessConfigImpl.ENABLED, !ServerlessConfigImpl.DEFAULT_ENABLED);
        ServerlessConfig config = ServerlessConfigImpl.createServerlessConfig(localMap);

        Assert.assertEquals(!ServerlessConfigImpl.DEFAULT_ENABLED, config.isEnabled());

        localMap.clear();
        localMap.put(ServerlessConfigImpl.ENABLED, Boolean.TRUE);
        config = ServerlessConfigImpl.createServerlessConfig(localMap);
        Assert.assertEquals(Boolean.TRUE, config.isEnabled());
    }

    @Test
    public void isEnabledSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = ServerlessConfigImpl.SYSTEM_PROPERTY_ROOT + ServerlessConfigImpl.ENABLED;
        String val = String.valueOf(!ServerlessConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ServerlessConfigImpl.ENABLED, ServerlessConfigImpl.DEFAULT_ENABLED);
        ServerlessConfig config = ServerlessConfigImpl.createServerlessConfig(localMap);

        Assert.assertEquals(!ServerlessConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = ServerlessConfigImpl.SYSTEM_PROPERTY_ROOT + ServerlessConfigImpl.ENABLED;
        String val = String.valueOf(!ServerlessConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(ServerlessConfigImpl.DEFAULT_ENABLED);
        localMap.put(ServerlessConfigImpl.ENABLED, serverProp);
        ServerlessConfig config = ServerlessConfigImpl.createServerlessConfig(localMap);

        Assert.assertEquals(ServerlessConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }


    @Test
    public void getOffJmxFrameworks() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ServerlessConfigImpl.FILE_PATH, "/tmp/some-file");
        ServerlessConfig config = ServerlessConfigImpl.createServerlessConfig(localMap);

        Assert.assertEquals("/tmp/some-file", config.filePath());
    }

    @Test
    public void checkDefaults() {
        Map<String, Object> localMap = new HashMap<>();
        ServerlessConfig config = ServerlessConfigImpl.createServerlessConfig(localMap);

        Assert.assertEquals(ServerlessConfigImpl.DEFAULT_ENABLED, false);
        Assert.assertEquals(ServerlessConfigImpl.DEFAULT_ENABLED, config.isEnabled());
        Assert.assertEquals(ServerlessConfigImpl.DEFAULT_FILE_PATH, "/tmp/newrelic-telemetry");
        Assert.assertEquals(ServerlessConfigImpl.DEFAULT_FILE_PATH, config.filePath());
    }

    @Test
    public void newRelicServerlessModeEnabledTrueWinsRegardlessOfLambdaEnvVar() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("newrelic.config.serverless_mode.enabled", "true");
        Mocks.createSystemPropertyProvider(new HashMap<>(), envVars);
        ServerlessConfigImpl config = new ServerlessConfigImpl(Collections.emptyMap(), false);

        Assert.assertTrue(config.isEnabled());
    }

    @Test
    public void newRelicServerlessModeEnabledFalseWinsRegardlessOfLambdaEnvVar() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("newrelic.config.serverless_mode.enabled", "false");
        Mocks.createSystemPropertyProvider(new HashMap<>(), envVars);
        ServerlessConfigImpl config = new ServerlessConfigImpl(Collections.emptyMap(), true);

        Assert.assertFalse(config.isEnabled());
    }

    @Test
    public void awsLambdaFunctionNameEnablesServerlessMode() {
        ServerlessConfigImpl config = new ServerlessConfigImpl(Collections.emptyMap(), true);

        Assert.assertTrue(config.isEnabled());
    }

    @Test
    public void explicitFalseOverridesAwsLambdaFunctionName() {
        Map<String, Object> props = new HashMap<>();
        props.put(ServerlessConfigImpl.ENABLED, false);
        ServerlessConfigImpl config = new ServerlessConfigImpl(props, true);

        Assert.assertFalse(config.isEnabled());
    }

    @Test
    public void explicitTrueWithNoLambdaEnvVar() {
        Map<String, Object> props = new HashMap<>();
        props.put(ServerlessConfigImpl.ENABLED, true);
        ServerlessConfigImpl config = new ServerlessConfigImpl(props, false);

        Assert.assertTrue(config.isEnabled());
    }

    @Test
    public void noConfigAndNoLambdaEnvVarDefaultsToDisabled() {
        ServerlessConfigImpl config = new ServerlessConfigImpl(Collections.emptyMap(), false);

        Assert.assertFalse(config.isEnabled());
    }
}
