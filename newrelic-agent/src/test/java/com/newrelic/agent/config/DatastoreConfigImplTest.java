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

public class DatastoreConfigImplTest {

    public final String DOT=".";
    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isInstanceEnabled() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        localMap.put(DatastoreConfigImpl.INSTANCE_REPORTING, nestedMap);
        nestedMap.put(DatastoreConfigImpl.ENABLED, !DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        DatastoreConfig config = new DatastoreConfigImpl(localMap);

        Assert.assertEquals(!DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED, config.isInstanceReportingEnabled());
    }

    @Test
    public void isDatabaseNameEnabled() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        localMap.put(DatastoreConfigImpl.DATABASE_NAME_REPORTING, nestedMap);
        nestedMap.put(DatastoreConfigImpl.ENABLED, !DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED);
        DatastoreConfig config = new DatastoreConfigImpl(localMap);

        Assert.assertEquals(!DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED, config.isDatabaseNameReportingEnabled());
    }

    @Test
    public void isInstanceEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = DatastoreConfigImpl.PROPERTY_ROOT + DatastoreConfigImpl.INSTANCE_REPORTING+DOT+DatastoreConfigImpl.ENABLED;
        String val = String.valueOf(!DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        localMap.put(DatastoreConfigImpl.INSTANCE_REPORTING, nestedMap);
        nestedMap.put(DatastoreConfigImpl.ENABLED, DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        DatastoreConfig config = new DatastoreConfigImpl(localMap);

        Assert.assertEquals(!DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED, config.isInstanceReportingEnabled());
    }

    @Test
    public void isDatabaseNameEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = DatastoreConfigImpl.PROPERTY_ROOT + DatastoreConfigImpl.DATABASE_NAME_REPORTING+DOT+DatastoreConfigImpl.ENABLED;
        String val = String.valueOf(!DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        localMap.put(DatastoreConfigImpl.DATABASE_NAME_REPORTING, nestedMap);
        nestedMap.put(DatastoreConfigImpl.ENABLED, DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED);
        DatastoreConfig config = new DatastoreConfigImpl(localMap);

        Assert.assertEquals(!DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED, config.isDatabaseNameReportingEnabled());
    }

    @Test
    public void isInstanceEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = DatastoreConfigImpl.PROPERTY_ROOT + DatastoreConfigImpl.INSTANCE_REPORTING+DOT+DatastoreConfigImpl.ENABLED;
        String val = String.valueOf(!DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        localMap.put(DatastoreConfigImpl.INSTANCE_REPORTING, nestedMap);
        ServerProp serverProp = ServerProp.createPropObject(DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        nestedMap.put(DatastoreConfigImpl.ENABLED, serverProp);
        DatastoreConfig config = new DatastoreConfigImpl(localMap);

        Assert.assertEquals(DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED, config.isInstanceReportingEnabled());
    }

    @Test
    public void isDatabaseNameEnabledServerSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = DatastoreConfigImpl.PROPERTY_ROOT + DatastoreConfigImpl.DATABASE_NAME_REPORTING+DOT+DatastoreConfigImpl.ENABLED;
        String val = String.valueOf(!DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        localMap.put(DatastoreConfigImpl.DATABASE_NAME_REPORTING, nestedMap);
        ServerProp serverProp = ServerProp.createPropObject(DatastoreConfigImpl.INSTANCE_REPORTING_DEFAULT_ENABLED);
        nestedMap.put(DatastoreConfigImpl.ENABLED, serverProp);
        DatastoreConfig config = new DatastoreConfigImpl(localMap);

        Assert.assertEquals(DatastoreConfigImpl.DATABASE_NAME_REPORTING_DEFAULT_ENABLED, config.isDatabaseNameReportingEnabled());
    }
}
