/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TransactionCountsTest {

    @Test
    public void testTransactionCountsReachSizeLimit() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> ttprops = new HashMap<>();
        settings.put(AgentConfigImpl.TRANSACTION_SIZE_LIMIT, 1); // gets multipled by 1024
        settings.put(AgentConfigImpl.TRANSACTION_TRACER, ttprops);
        ttprops.put(TransactionTracerConfigImpl.SEGMENT_LIMIT, 20);
        AgentConfig config = AgentConfigFactory.createAgentConfig(settings, null, null);
        TransactionCounts counts = new TransactionCounts(config);
        Assert.assertFalse(counts.isOverTracerSegmentLimit());
        Assert.assertFalse(counts.isOverTransactionSize());
        Assert.assertTrue(counts.shouldGenerateTransactionSegment());

        for (int i = 0; i < 9; i++) {
            counts.addTracer();
        }

        Assert.assertFalse(counts.isOverTracerSegmentLimit());
        Assert.assertTrue(counts.isOverTransactionSize());
        Assert.assertFalse(counts.shouldGenerateTransactionSegment());
        Assert.assertEquals(1152, counts.getTransactionSize());
    }

    @Test
    public void testTransactionCountsReachSegmentLimit() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> ttprops = new HashMap<>();
        settings.put(AgentConfigImpl.TRANSACTION_SIZE_LIMIT, 100); // gets multipled by 1024
        settings.put(AgentConfigImpl.TRANSACTION_TRACER, ttprops);
        ttprops.put(TransactionTracerConfigImpl.SEGMENT_LIMIT, 5);
        AgentConfig config = AgentConfigFactory.createAgentConfig(settings, null, null);
        TransactionCounts counts = new TransactionCounts(config);
        Assert.assertFalse(counts.isOverTracerSegmentLimit());
        Assert.assertFalse(counts.isOverTransactionSize());
        Assert.assertTrue(counts.shouldGenerateTransactionSegment());

        for (int i = 0; i < 6; i++) {
            counts.addTracer();
        }

        Assert.assertTrue(counts.isOverTracerSegmentLimit());
        Assert.assertFalse(counts.isOverTransactionSize());
        Assert.assertFalse(counts.shouldGenerateTransactionSegment());
        Assert.assertEquals(6, counts.getSegmentCount());
    }

}
