/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ExternalTracerConfigImplTest {

    @After
    public void after() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider());
    }

    @Test
    public void excludeRequestUri() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(ExternalTracerConfigImpl.EXCLUDE_REQUEST_URI, !ExternalTracerConfigImpl.DEFAULT_EXCLUDE_REQUEST_URI);
        ExternalTracerConfig config = new ExternalTracerConfigImpl(localMap);

        Assert.assertEquals(!ExternalTracerConfigImpl.DEFAULT_EXCLUDE_REQUEST_URI, config.excludeRequestUri());
    }
}
