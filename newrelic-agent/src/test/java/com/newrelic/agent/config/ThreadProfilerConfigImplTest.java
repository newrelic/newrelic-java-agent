/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Mocks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ThreadProfilerConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isProfilingEnabled() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(ThreadProfilerConfigImpl.ENABLED, !ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        ThreadProfilerConfig config = ThreadProfilerConfigImpl.createThreadProfilerConfig(localSettings);

        Assert.assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isProfilingEnabledServer() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(ThreadProfilerConfigImpl.ENABLED, !ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        ThreadProfilerConfig config = ThreadProfilerConfigImpl.createThreadProfilerConfig(serverSettings);

        Assert.assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isProfilingEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ThreadProfilerConfigImpl.SYSTEM_PROPERTY_ROOT + ThreadProfilerConfigImpl.ENABLED;
        String val = String.valueOf(ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> serverSettings = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        serverSettings.put(ThreadProfilerConfigImpl.ENABLED, serverProp);
        ThreadProfilerConfig config = ThreadProfilerConfigImpl.createThreadProfilerConfig(serverSettings);

        Assert.assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isProfilingEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ThreadProfilerConfigImpl.SYSTEM_PROPERTY_ROOT + ThreadProfilerConfigImpl.ENABLED;
        String val = String.valueOf(!ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(ThreadProfilerConfigImpl.ENABLED, ThreadProfilerConfigImpl.DEFAULT_ENABLED);
        ThreadProfilerConfig config = ThreadProfilerConfigImpl.createThreadProfilerConfig(localSettings);

        Assert.assertEquals(!ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isProfilingEnabledDefault() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        ThreadProfilerConfig config = ThreadProfilerConfigImpl.createThreadProfilerConfig(localSettings);

        Assert.assertEquals(ThreadProfilerConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

}
