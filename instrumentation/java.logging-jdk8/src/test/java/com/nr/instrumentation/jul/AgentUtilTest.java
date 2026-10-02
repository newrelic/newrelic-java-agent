/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.jul;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import com.newrelic.api.agent.Logs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.INSTRUMENTATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentUtilTest {

    private final Agent originalAgent = AgentBridge.getAgent();
    private final Agent mockAgent = mock(Agent.class);
    private final Logs mockLogs = mock(Logs.class);

    @Before
    public void before() {
        AgentBridge.agent = mockAgent;
        when(mockAgent.getLogSender()).thenReturn(mockLogs);
    }

    @After
    public void after() {
        AgentBridge.agent = originalAgent;
    }

    @Test
    public void testCustomLogAttributesAreAddedToLogEvent() {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("env", "prod");
        when(mockAgent.getConfiguredCustomLogAttributes()).thenReturn(customAttributes);

        AgentUtil.recordNewRelicLogEvent(new LogRecord(Level.SEVERE, "message"));

        Map<LogAttributeKey, Object> logEventMap = captureLoggedMap();
        assertEquals("prod", logEventMap.get(new LogAttributeKey("env", LogAttributeType.AGENT)));
    }

    @Test
    public void testCustomLogAttributesDoNotOverrideStandardAttributes() {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put(INSTRUMENTATION.getKey(), "customInstrumentationValue");
        when(mockAgent.getConfiguredCustomLogAttributes()).thenReturn(customAttributes);

        AgentUtil.recordNewRelicLogEvent(new LogRecord(Level.SEVERE, "message"));

        Map<LogAttributeKey, Object> logEventMap = captureLoggedMap();
        assertEquals("java.logging-jdk8", logEventMap.get(INSTRUMENTATION));
    }

    @Test
    public void testNoCustomLogAttributesConfigured_doesNotAddAnyAttributes() {
        when(mockAgent.getConfiguredCustomLogAttributes()).thenReturn(Collections.emptyMap());

        AgentUtil.recordNewRelicLogEvent(new LogRecord(Level.SEVERE, "message"));

        Map<LogAttributeKey, Object> logEventMap = captureLoggedMap();
        assertNull(logEventMap.get(new LogAttributeKey("env", LogAttributeType.AGENT)));
    }

    @SuppressWarnings("unchecked")
    private Map<LogAttributeKey, Object> captureLoggedMap() {
        ArgumentCaptor<Map<LogAttributeKey, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockLogs).recordLogEvent(captor.capture());
        return captor.getValue();
    }
}
