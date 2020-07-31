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

public class ReinstrumentConfigImplTest {
    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ReinstrumentConfigImpl.ENABLED, !ReinstrumentConfigImpl.DEFAULT_ENABLED);
        ReinstrumentConfig config = ReinstrumentConfigImpl.createReinstrumentConfig(localMap);

        Assert.assertEquals(!ReinstrumentConfigImpl.DEFAULT_ENABLED, config.isEnabled());

        localMap.clear();
        localMap.put(ReinstrumentConfigImpl.ENABLED, Boolean.TRUE);
        config = ReinstrumentConfigImpl.createReinstrumentConfig(localMap);
        Assert.assertEquals(Boolean.TRUE, config.isEnabled());
    }

    @Test
    public void isEnabledSystemProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        String key = ReinstrumentConfigImpl.SYSTEM_PROPERTY_ROOT + ReinstrumentConfigImpl.ENABLED;
        String val = String.valueOf(!ReinstrumentConfigImpl.DEFAULT_ENABLED);
        properties.put(key, val);
        Mocks.createSystemPropertyProvider(properties);
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ReinstrumentConfigImpl.ENABLED, ReinstrumentConfigImpl.DEFAULT_ENABLED);
        ReinstrumentConfig config = ReinstrumentConfigImpl.createReinstrumentConfig(localMap);

        Assert.assertEquals(!ReinstrumentConfigImpl.DEFAULT_ENABLED, config.isEnabled());
    }

    @Test
    public void isAttributesEnabled() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ReinstrumentConfigImpl.ATTS_ENABLED, !ReinstrumentConfigImpl.DEFAULT_ATTS_ENABLED);
        ReinstrumentConfig config = ReinstrumentConfigImpl.createReinstrumentConfig(localMap);

        Assert.assertEquals(!ReinstrumentConfigImpl.DEFAULT_ATTS_ENABLED, config.isAttributesEnabled());

        localMap.clear();
        localMap.put(ReinstrumentConfigImpl.ATTS_ENABLED, Boolean.FALSE);
        config = ReinstrumentConfigImpl.createReinstrumentConfig(localMap);
        Assert.assertEquals(Boolean.FALSE, config.isAttributesEnabled());

        localMap.clear();
        config = ReinstrumentConfigImpl.createReinstrumentConfig(localMap);
        Assert.assertEquals(Boolean.FALSE, config.isAttributesEnabled());
    }

}
