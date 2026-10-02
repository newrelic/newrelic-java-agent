/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.utils.logs;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.TestLoggerBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.INSTRUMENTATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomLogAttributesTest {

    private final Agent originalAgent = AgentBridge.getAgent();
    private final Agent mockAgent = mock(Agent.class);
    private final Logs mockLogs = mock(Logs.class);
    private MockedStatic<NewRelic> mockNewRelic;

    @Before
    public void before() {
        AgentBridge.agent = mockAgent;
        when(mockAgent.getLogSender()).thenReturn(mockLogs);

        com.newrelic.api.agent.Agent mockApiAgent = mock(com.newrelic.api.agent.Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockApiAgent.getConfig().getValue(anyString(), anyBoolean())).thenReturn(false);
        mockNewRelic = Mockito.mockStatic(NewRelic.class);
        mockNewRelic.when(NewRelic::getAgent).thenReturn(mockApiAgent);
    }

    @After
    public void after() {
        AgentBridge.agent = originalAgent;
        mockNewRelic.close();
    }

    @Test
    public void testCustomLogAttributesAreAddedToLogEvent() {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("env", "prod");
        when(mockAgent.getConfiguredCustomLogAttributes()).thenReturn(customAttributes);

        LogEventUtil.recordNewRelicLogEvent(buildLogRecordData("message"));

        Map<LogAttributeKey, Object> logEventMap = captureLoggedMap();
        assertEquals("prod", logEventMap.get(new LogAttributeKey("env", LogAttributeType.AGENT)));
    }

    @Test
    public void testCustomLogAttributesDoNotOverrideStandardAttributes() {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put(INSTRUMENTATION.getKey(), "customInstrumentationValue");
        when(mockAgent.getConfiguredCustomLogAttributes()).thenReturn(customAttributes);

        LogEventUtil.recordNewRelicLogEvent(buildLogRecordData("message"));

        Map<LogAttributeKey, Object> logEventMap = captureLoggedMap();
        assertEquals("opentelemetry-sdk-extension-autoconfigure-1.59.0", logEventMap.get(INSTRUMENTATION));
    }

    @Test
    public void testNoCustomLogAttributesConfigured_doesNotAddAnyAttributes() {
        when(mockAgent.getConfiguredCustomLogAttributes()).thenReturn(java.util.Collections.emptyMap());

        LogEventUtil.recordNewRelicLogEvent(buildLogRecordData("message"));

        Map<LogAttributeKey, Object> logEventMap = captureLoggedMap();
        assertNull(logEventMap.get(new LogAttributeKey("env", LogAttributeType.AGENT)));
    }

    @SuppressWarnings("unchecked")
    private Map<LogAttributeKey, Object> captureLoggedMap() {
        ArgumentCaptor<Map<LogAttributeKey, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockLogs).recordLogEvent(captor.capture());
        return captor.getValue();
    }

    private static LogRecordData buildLogRecordData(String body) {
        List<ReadWriteLogRecord> emitted = new ArrayList<>();
        LogRecordProcessor logRecordProcessor = new LogRecordProcessor() {
            @Override
            public void onEmit(Context context, ReadWriteLogRecord logRecord) {
                emitted.add(logRecord);
            }

            @Override
            public CompletableResultCode shutdown() {
                return LogRecordProcessor.super.shutdown();
            }

            @Override
            public CompletableResultCode forceFlush() {
                return LogRecordProcessor.super.forceFlush();
            }

            @Override
            public void close() {
                LogRecordProcessor.super.close();
            }
        };

        io.opentelemetry.api.logs.Logger logger = new TestLoggerBuilder("test")
                .addLogRecordProcessor(logRecordProcessor)
                .setResource(Resource.getDefault())
                .build();

        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setBody(body)
                .emit();

        return emitted.get(0).toLogRecordData();
    }
}