/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionEventsConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TransactionEventsConfigTest {

    @Test
    public void testMaxSamplesStores() {
        // setup
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> transaction = new HashMap<>();
        root.put("transaction_events", transaction);

        // start with the default
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        assertEquals(TransactionEventsConfig.DEFAULT_MAX_SAMPLES_STORED, config.getTransactionEventsConfig().getMaxSamplesStored());

        // normal old config
        transaction.clear();
        config = AgentConfigImpl.createAgentConfig(root);
        assertEquals(TransactionEventsConfig.DEFAULT_MAX_SAMPLES_STORED, config.getTransactionEventsConfig().getMaxSamplesStored());

        // normal new config
        transaction.clear();
        transaction.put("max_samples_stored", 10);
        config = AgentConfigImpl.createAgentConfig(root);
        assertEquals(10, config.getTransactionEventsConfig().getMaxSamplesStored());

        // over new config
        transaction.clear();
        transaction.put("max_samples_stored", 100000000);
        config = AgentConfigImpl.createAgentConfig(root);
        assertEquals(100000000, config.getTransactionEventsConfig().getMaxSamplesStored());
    }

    @Test
    public void testEnabled() {
        // setup
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> transaction = new HashMap<>();
        root.put("transaction_events", transaction);

        // start with the default
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertTrue(config.getTransactionEventsConfig().isEnabled());

        // no samples
        transaction.put(TransactionEventsConfig.MAX_SAMPLES_STORED, 0);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertFalse(config.getTransactionEventsConfig().isEnabled());

        transaction.clear();
        transaction.put("enabled", false);
        transaction.put(TransactionEventsConfig.MAX_SAMPLES_STORED, 10);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertFalse(config.getTransactionEventsConfig().isEnabled());

        transaction.clear();
        transaction.put("collect_analytics_events", false);
        transaction.put(TransactionEventsConfig.MAX_SAMPLES_STORED, 10);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertFalse(config.getTransactionEventsConfig().isEnabled());

        // set both
        transaction.clear();
        transaction.put("enabled", true);
        transaction.put("collect_transaction_events", true);
        transaction.put(TransactionEventsConfig.MAX_SAMPLES_STORED, 10);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertTrue(config.getTransactionEventsConfig().isEnabled());

        transaction.clear();
        transaction.put("enabled", false);
        transaction.put("collect_analytics_events", true);
        transaction.put(TransactionEventsConfig.MAX_SAMPLES_STORED, 10);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertFalse(config.getTransactionEventsConfig().isEnabled());
    }

    @Test
    public void createTransactionEventConfig_withNullSettingsInstance_returnsInstanceWithDefaultProps() {
        TransactionEventsConfig config = TransactionEventsConfig.createTransactionEventConfig(null);
        Assert.assertEquals(2000, config.getMaxSamplesStored());
        Assert.assertEquals(10, config.getTargetSamplesStored());
        Assert.assertEquals(0, config.getRequestHeaderConfigs().size());
        Assert.assertTrue(config.isEnabled());
    }
}
