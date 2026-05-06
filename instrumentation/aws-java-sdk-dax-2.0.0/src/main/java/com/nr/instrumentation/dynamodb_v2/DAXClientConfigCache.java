/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.dynamodb_v2;

import com.newrelic.agent.bridge.AgentBridge;
import software.amazon.dax.Configuration;

import java.util.Map;
import java.util.logging.Level;

/**
 * Caches DAX Configuration objects by client instance.
 * <p>
 * DAX clients don't expose their Configuration after construction, so we capture it
 * during MetricAsyncClient construction and cache it for later use in metrics reporting.
 */
public class DAXClientConfigCache {

    private static final Map<Object, Configuration> clientConfigurations =
            AgentBridge.collectionFactory.createWeakKeyedCacheWithInitialCapacity(4);

    /**
     * Store the Configuration for a client instance.
     * Called during MetricAsyncClient constructor.
     */
    public static void storeConfiguration(Object client, Configuration configuration) {
        if (client != null && configuration != null) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Adding client configuration: {0}", configuration);
            clientConfigurations.put(client, configuration);
        }
    }

    /**
     * Retrieve the Configuration for a client instance.
     * Returns null if no Configuration is cached for this client.
     */
    public static Configuration getConfiguration(Object client) {
        if (client == null) {
            return null;
        }
        return clientConfigurations.get(client);
    }
}
