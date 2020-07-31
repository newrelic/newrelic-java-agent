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

public class JmxConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabled() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(JmxConfigImpl.ENABLED, !JmxConfigImpl.DEFAULT_ENABLED);
        JmxConfig config = JmxConfigImpl.createJmxConfig(localMap);

        Assert.assertEquals(!JmxConfigImpl.DEFAULT_ENABLED, config.isEnabled());

        localMap.clear();
        localMap.put(JmxConfigImpl.ENABLED, Boolean.TRUE);
        config = JmxConfigImpl.createJmxConfig(localMap);
        Assert.assertEquals(Boolean.TRUE, config.isEnabled());
    }

    @Test
    public void isEnabledSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = JmxConfigImpl.SYSTEM_PROPERTY_ROOT + JmxConfigImpl.ENABLED;
        String val = String.valueOf(!JmxConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(JmxConfigImpl.ENABLED, JmxConfigImpl.DEFAULT_ENABLED);
        JmxConfig config = JmxConfigImpl.createJmxConfig(localMap);

        Assert.assertEquals(!JmxConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledServerSystemProperty() {
        Map<String, String> properties = new HashMap<>();
        String key = JmxConfigImpl.SYSTEM_PROPERTY_ROOT + JmxConfigImpl.ENABLED;
        String val = String.valueOf(!JmxConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(JmxConfigImpl.DEFAULT_ENABLED);
        localMap.put(JmxConfigImpl.ENABLED, serverProp);
        JmxConfig config = JmxConfigImpl.createJmxConfig(localMap);

        Assert.assertEquals(JmxConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }


    @Test
    public void getOffJmxFrameworks() {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(JmxConfigImpl.DISABLED_JMX_FRAMEWORKS, "Catalina, java.lang");
        JmxConfig config = JmxConfigImpl.createJmxConfig(localMap);

        Assert.assertEquals(2, config.getDisabledJmxFrameworks().size());
        Assert.assertEquals("Catalina", config.getDisabledJmxFrameworks().toArray()[0]);
        Assert.assertEquals("java.lang", config.getDisabledJmxFrameworks().toArray()[1]);
    }

    @Test
    public void checkDefaults() {
        Map<String, Object> localMap = new HashMap<>();
        JmxConfig config = JmxConfigImpl.createJmxConfig(localMap);

        Assert.assertEquals(JmxConfigImpl.DEFAULT_ENABLED, config.isEnabled());
        Assert.assertEquals(0, config.getDisabledJmxFrameworks().size());
    }

}
