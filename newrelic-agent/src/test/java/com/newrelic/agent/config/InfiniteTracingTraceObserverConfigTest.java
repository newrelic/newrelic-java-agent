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

public class InfiniteTracingTraceObserverConfigTest {
    private Map<String, Object> localProps;
    private static final String TEST_HOST = "a non-empty string";
    private static final int TEST_PORT = 8080;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
        localProps.put(InfiniteTracingTraceObserverConfig.HOST, TEST_HOST);
        localProps.put(InfiniteTracingTraceObserverConfig.PORT, TEST_PORT);
    }

    @Test
    public void testHostAndPortShouldBeSet() {
        InfiniteTracingTraceObserverConfig config = new InfiniteTracingTraceObserverConfig(localProps, "parent_root.");
        assertEquals(TEST_HOST, config.getHost());
        assertEquals(TEST_PORT, config.getPort());
    }

    @Test
    public void testHostShouldBeDefault() {
        localProps.put(InfiniteTracingTraceObserverConfig.HOST, null);
        InfiniteTracingTraceObserverConfig config = new InfiniteTracingTraceObserverConfig(localProps, "parent_root.");
        assertEquals(InfiniteTracingTraceObserverConfig.DEFAULT_HOST, config.getHost());
    }

    @Test
    public void testPortShouldBeDefault() {
        localProps.put(InfiniteTracingTraceObserverConfig.PORT, null);
        InfiniteTracingTraceObserverConfig config = new InfiniteTracingTraceObserverConfig(localProps, "parent_root.");
        assertEquals(InfiniteTracingTraceObserverConfig.DEFAULT_PORT, config.getPort());
    }

    @Test
    public void usesParentRootForNestedConfig() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap("PARENT_ROOT_TRACE_OBSERVER_HOST", "foobar"))
        ));

        InfiniteTracingTraceObserverConfig config = new InfiniteTracingTraceObserverConfig(Collections.<String, Object>emptyMap(), "parent_root.");
        assertEquals("foobar", config.getHost());
    }

    @Test
    public void usesParentRootForNestedProps() {
        Properties props = new Properties();
        props.put("parent_root.trace_observer.host", "foobar");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        InfiniteTracingTraceObserverConfig config = new InfiniteTracingTraceObserverConfig(Collections.<String, Object>emptyMap(), "parent_root.");
        assertEquals("foobar", config.getHost());
    }
}