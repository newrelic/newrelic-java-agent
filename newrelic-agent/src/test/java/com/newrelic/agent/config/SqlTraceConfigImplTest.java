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

public class SqlTraceConfigImplTest {
    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(SqlTraceConfigImpl.ENABLED, !SqlTraceConfigImpl.DEFAULT_ENABLED);
        SqlTraceConfig config = SqlTraceConfigImpl.createSqlTraceConfig(localMap);

        Assert.assertEquals(!SqlTraceConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = SqlTraceConfigImpl.SYSTEM_PROPERTY_ROOT + SqlTraceConfigImpl.ENABLED;
        String val = String.valueOf(!SqlTraceConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(SqlTraceConfigImpl.ENABLED, SqlTraceConfigImpl.DEFAULT_ENABLED);
        SqlTraceConfig config = SqlTraceConfigImpl.createSqlTraceConfig(localMap);

        Assert.assertEquals(!SqlTraceConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = SqlTraceConfigImpl.SYSTEM_PROPERTY_ROOT + SqlTraceConfigImpl.ENABLED;
        String val = String.valueOf(!SqlTraceConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(SqlTraceConfigImpl.DEFAULT_ENABLED);
        localMap.put(SqlTraceConfigImpl.ENABLED, serverProp);
        SqlTraceConfig config = SqlTraceConfigImpl.createSqlTraceConfig(localMap);

        Assert.assertEquals(SqlTraceConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isEnabledDefault() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        SqlTraceConfig config = SqlTraceConfigImpl.createSqlTraceConfig(localMap);

        Assert.assertEquals(SqlTraceConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

}
