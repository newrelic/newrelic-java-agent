/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class InfiniteTracingSpanEventsConfigTest {
    private Map<String, Object> localProps;
    private static final int TEST_QUEUE_SIZE = 1000;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
        localProps.put(InfiniteTracingSpanEventsConfig.QUEUE_SIZE, TEST_QUEUE_SIZE);
    }

    @Test
    public void testQueueSizeShouldBe1000() {
        InfiniteTracingSpanEventsConfig config = new InfiniteTracingSpanEventsConfig(localProps, "parent_root.");
        assertEquals(TEST_QUEUE_SIZE, config.getQueueSize());
    }

    @Test
    public void testQueueSizeShouldBeDefault() {
        localProps.put(InfiniteTracingSpanEventsConfig.QUEUE_SIZE, null);
        InfiniteTracingSpanEventsConfig config = new InfiniteTracingSpanEventsConfig(localProps, "parent_root.");
        assertEquals(InfiniteTracingSpanEventsConfig.DEFAULT_SPAN_EVENTS_QUEUE_SIZE, config.getQueueSize());
    }

    @Test
    public void usesParentRootForNestedConfig() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap("PARENT_ROOT_SPAN_EVENTS_QUEUE_SIZE", "34534"))
        ));

        InfiniteTracingSpanEventsConfig config = new InfiniteTracingSpanEventsConfig(Collections.<String, Object>emptyMap(), "parent_root.");
        assertEquals(34534, config.getQueueSize());
    }

    @Test
    public void usesParentRootForNestedProps() {
        Properties properties = new Properties();
        properties.put("parent_root.span_events.queue_size", "34534");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        InfiniteTracingSpanEventsConfig config = new InfiniteTracingSpanEventsConfig(Collections.<String, Object>emptyMap(), "parent_root.");
        assertEquals(34534, config.getQueueSize());
    }
}