/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.InfiniteTracingConfig;
import com.newrelic.agent.config.SpanEventsConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InfiniteTracingEnabledCheckTest {
    private ConfigService configService;
    private AgentConfig agentConfig;
    private InfiniteTracingConfig infiniteTracingConfig;
    private SpanEventsConfig spanEventsConfig;

    @Before
    public void setup(){
        configService = mock(ConfigService.class);
        agentConfig = mock(AgentConfig.class);
        infiniteTracingConfig = mock(InfiniteTracingConfig.class);
        spanEventsConfig = mock(SpanEventsConfig.class);
        when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
        when(agentConfig.getInfiniteTracingConfig()).thenReturn(infiniteTracingConfig);
        when(agentConfig.getSpanEventsConfig()).thenReturn(spanEventsConfig);
    }

    @Test
    public void testIsEnabled() throws Exception {
        InfiniteTracingEnabledCheck testClass = new InfiniteTracingEnabledCheck(configService);
        when(infiniteTracingConfig.isEnabled()).thenReturn(true);
        assertTrue(testClass.isEnabled());
        when(infiniteTracingConfig.isEnabled()).thenReturn(false);
        assertFalse(testClass.isEnabled());
    }

    @Test
    public void testIsEnabledAndSpanEventsEnabled() throws Exception {
        when(infiniteTracingConfig.isEnabled()).thenReturn(true);
        when(spanEventsConfig.isEnabled()).thenReturn(true);
        InfiniteTracingEnabledCheck testClass = new InfiniteTracingEnabledCheck(configService);
        assertTrue(testClass.isEnabledAndSpanEventsEnabled());
    }

    @Test
    public void testIsDisabledAndSpanEventsDisabled() throws Exception {
        when(infiniteTracingConfig.isEnabled()).thenReturn(false);
        when(spanEventsConfig.isEnabled()).thenReturn(false);
        InfiniteTracingEnabledCheck testClass = new InfiniteTracingEnabledCheck(configService);
        assertFalse(testClass.isEnabledAndSpanEventsEnabled());
    }

    @Test
    public void testIsEnabledButSpanEventsDisabled() throws Exception {
        when(infiniteTracingConfig.isEnabled()).thenReturn(true);
        when(spanEventsConfig.isEnabled()).thenReturn(false);
        InfiniteTracingEnabledCheck testClass = new InfiniteTracingEnabledCheck(configService);
        assertFalse(testClass.isEnabledAndSpanEventsEnabled());
    }
}