/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Map;
import java.util.Map.Entry;

import org.mockito.Mockito;

import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.config.SystemPropertyProvider;

public class Mocks {
    private Mocks() {
    }

    public static SystemPropertyProvider createSystemPropertyProvider(Map<String, String> properties) {
        SystemPropertyProvider provider = Mockito.spy(new SystemPropertyProvider());
        SystemPropertyFactory.setSystemPropertyProvider(provider);

        for (Entry<String, String> entry : properties.entrySet()) {
            Mockito.when(provider.getSystemProperty(entry.getKey())).thenReturn(entry.getValue());
        }

        return provider;
    }

    public static SystemPropertyProvider createSystemPropertyFlattenedProvider(Map<String, Object> properties) {
        SystemPropertyProvider provider = Mockito.spy(new SystemPropertyProvider());
        SystemPropertyFactory.setSystemPropertyProvider(provider);

        Mockito.when(provider.getNewRelicPropertiesWithoutPrefix()).thenReturn(properties);

        return provider;
    }

    public static SystemPropertyProvider createSystemPropertyProvider(Map<String, String> properties,
            Map<String, String> environment) {
        SystemPropertyProvider provider = createSystemPropertyProvider(properties);

        for (Entry<String, String> entry : environment.entrySet()) {
            Mockito.when(provider.getEnvironmentVariable(entry.getKey())).thenReturn(entry.getValue());
        }

        return provider;
    }
}
