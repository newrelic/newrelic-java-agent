/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.newrelic.agent.config.InfiniteTracingConfigImpl.FLAKY_PERCENTAGE;
import static com.newrelic.agent.config.InfiniteTracingConfigImpl.TRACE_OBSERVER;
import static com.newrelic.agent.config.InfiniteTracingTraceObserverConfig.HOST;
import static com.newrelic.agent.config.InfiniteTracingTraceObserverConfig.PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InfiniteTracingConfigImplTest {
    private Map<String, Object> localProps;
    private Map<String, Object> traceObserverProps;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
        traceObserverProps = new HashMap<>();
        traceObserverProps.put(HOST, "not empty");
        traceObserverProps.put(PORT, 8080);
        localProps.put(TRACE_OBSERVER, traceObserverProps);
    }

    @Test
    public void testShouldBeEnabled() {
        InfiniteTracingConfigImpl config = new InfiniteTracingConfigImpl(localProps);
        assertTrue(config.isEnabled());
    }

    @Test
    public void testShouldBeDisabled() {
        traceObserverProps.put(HOST, null);
        InfiniteTracingConfigImpl config = new InfiniteTracingConfigImpl(localProps);
        assertFalse(config.isEnabled());
    }

    @Test
    public void testFlakyIs50() {
        localProps.put(FLAKY_PERCENTAGE, 50.0);
        InfiniteTracingConfigImpl config = new InfiniteTracingConfigImpl(localProps);
        assertEquals(50.0, config.getFlakyPercentage(), 0.0);
    }

    @Test
    public void testUseBatchingByDefault() {
        InfiniteTracingConfigImpl config = new InfiniteTracingConfigImpl(localProps);
        assertTrue(config.getUseBatching());
    }

    @Test
    public void testUseCompressionByDefault() {
        InfiniteTracingConfigImpl config = new InfiniteTracingConfigImpl(localProps);
        assertTrue(config.getUseCompression());
    }

    @Test
    public void canConfigureViaSystemPropertiesAndEnvironmentVariables() {
        Properties properties = new Properties();
        properties.put("newrelic.config.infinite_tracing.span_events.queue_size", "123433");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap("NEW_RELIC_INFINITE_TRACING_TRACE_OBSERVER_HOST", "flerbjoze"))
        ));

        InfiniteTracingConfigImpl config = new InfiniteTracingConfigImpl(Collections.<String, Object>emptyMap());
        assertEquals(123433, config.getSpanEventsQueueSize());
        assertEquals("flerbjoze", config.getTraceObserverHost());
    }

}
