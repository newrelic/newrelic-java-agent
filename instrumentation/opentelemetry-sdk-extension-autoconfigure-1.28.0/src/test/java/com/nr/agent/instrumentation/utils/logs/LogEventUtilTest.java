/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.logs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.NRLogRecord.BasicLogRecordData;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import junit.framework.TestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogEventUtilTest extends TestCase {

    private static final long TIMESTAMP_NANOS = 1_700_000_000_123_000_000L;
    private static final long OBSERVED_TIMESTAMP_NANOS = 1_700_000_005_456_000_000L;
    // A LogEvent timestamp is in epoch milliseconds, so the sub-millisecond part is dropped
    private static final long TIMESTAMP_MILLIS = 1_700_000_000_123L;
    private static final long OBSERVED_TIMESTAMP_MILLIS = 1_700_000_005_456L;

    public void testUsesTimestampWhenPresent() {
        assertEquals(Long.valueOf(TIMESTAMP_MILLIS), recordedTimestamp(TIMESTAMP_NANOS, OBSERVED_TIMESTAMP_NANOS));
    }

    // OTel represents an unset Timestamp as 0, so the ObservedTimestamp has to be used instead
    public void testFallsBackToObservedTimestampWhenTimestampUnset() {
        assertEquals(Long.valueOf(OBSERVED_TIMESTAMP_MILLIS), recordedTimestamp(0L, OBSERVED_TIMESTAMP_NANOS));
    }

    @SuppressWarnings("unchecked")
    private static Long recordedTimestamp(long timestampNanos, long observedTimestampNanos) {
        Logs logs = mock(Logs.class);
        com.newrelic.agent.bridge.Agent bridgeAgent = mock(com.newrelic.agent.bridge.Agent.class);
        when(bridgeAgent.getLogSender()).thenReturn(logs);

        // Use each setting's default value
        Config config = mock(Config.class);
        when(config.getValue(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        Agent apiAgent = mock(Agent.class);
        when(apiAgent.getConfig()).thenReturn(config);

        BasicLogRecordData logRecordData = mock(BasicLogRecordData.class);
        when(logRecordData.getTimestampEpochNanos()).thenReturn(timestampNanos);
        when(logRecordData.getObservedTimestampEpochNanos()).thenReturn(observedTimestampNanos);
        when(logRecordData.getBody()).thenReturn(Body.string("test message"));
        when(logRecordData.getAttributes()).thenReturn(Attributes.empty());
        when(logRecordData.getInstrumentationScopeInfo()).thenReturn(InstrumentationScopeInfo.create("test-scope"));

        try (MockedStatic<AgentBridge> mockBridge = Mockito.mockStatic(AgentBridge.class);
             MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockBridge.when(AgentBridge::getAgent).thenReturn(bridgeAgent);
            mockNewRelic.when(NewRelic::getAgent).thenReturn(apiAgent);

            LogEventUtil.recordNewRelicLogEvent(logRecordData);
        }

        ArgumentCaptor<Map<LogAttributeKey, ?>> captor = ArgumentCaptor.forClass((Class) Map.class);
        verify(logs).recordLogEvent(captor.capture());
        return (Long) captor.getValue().get(AppLoggingUtils.TIMESTAMP);
    }
}
