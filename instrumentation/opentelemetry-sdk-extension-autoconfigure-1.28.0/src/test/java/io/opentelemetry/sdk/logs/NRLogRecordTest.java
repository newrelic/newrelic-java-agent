/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.nr.agent.instrumentation.utils.AttributesHelper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import junit.framework.TestCase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NRLogRecordTest extends TestCase {
    static String exceptionMessage;
    static String exceptionType;
    static String exceptionStacktrace;

    public void testNRLogRecord() throws Exception {
        final List<ReadWriteLogRecord> emitted = new ArrayList<>();
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

        final String instrumentationScopeName = "test";
        final String body = "This is a test log message";
        final String severityText = "This is severity text";

        Logger logger = new TestLoggerBuilder(instrumentationScopeName).addLogRecordProcessor(logRecordProcessor)
                .setResource(Resource.getDefault())
                .setSchemaUrl("https://opentelemetry.io/schemas/1.0.0")
                .setInstrumentationVersion("1.0.0")
                .build();

        Map<String, Object> attributesMap = getAttributesMap();
        Attributes attributes = AttributesHelper.toAttributes(attributesMap);

        Instant now = Instant.now();
        logger.logRecordBuilder().
                setSeverity(Severity.INFO)
                .setSeverityText(severityText)
                .setBody(body)
                .setAllAttributes(attributes)
                .setAttribute(AttributeKey.stringKey("foo"), "bar")
                .setObservedTimestamp(now)
                .setObservedTimestamp(now.toEpochMilli(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setTimestamp(now)
                .setTimestamp(now.toEpochMilli(), java.util.concurrent.TimeUnit.MILLISECONDS)
//                .setContext()
                .emit();

        assertEquals(1, emitted.size());

        ReadWriteLogRecord readWriteLogRecord = emitted.get(0);
        LogRecordData logRecordData = readWriteLogRecord.toLogRecordData();

        assertEquals(instrumentationScopeName, logRecordData.getInstrumentationScopeInfo().getName());
        assertEquals("1.0.0", logRecordData.getInstrumentationScopeInfo().getVersion());
        assertEquals(instrumentationScopeName, logRecordData.getAttributes().get(AttributeKey.stringKey("otel.library.name")));
        assertEquals("1.0.0", logRecordData.getAttributes().get(AttributeKey.stringKey("otel.library.version")));

        // The +3 is because our instrumentation adds otel.library.version and
        // otel.library.name as attributes and one additional attribute is added via
        // the explicit setAttribute(AttributeKey.stringKey("foo"), "bar") call.
        assertEquals((attributesMap.size() + 3), logRecordData.getAttributes().size());
        assertEquals(4, logRecordData.getResource().getAttributes().size());
        assertEquals("opentelemetry", logRecordData.getResource().getAttributes().get(AttributeKey.stringKey("telemetry.sdk.name")));

        assertEquals(exceptionMessage, logRecordData.getAttributes().get(AttributeKey.stringKey("exception.message")));
        assertEquals(exceptionType, logRecordData.getAttributes().get(AttributeKey.stringKey("exception.type")));
        assertEquals(exceptionStacktrace, logRecordData.getAttributes().get(AttributeKey.stringKey("exception.stacktrace")));

        assertEquals("bar", logRecordData.getAttributes().get(AttributeKey.stringKey("foo")));

        assertEquals(body, logRecordData.getBody().asString());
        assertEquals(Severity.INFO, logRecordData.getSeverity());
        assertEquals(severityText, logRecordData.getSeverityText());

        long observedTimestampEpochNanos = TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano();
        assertEquals(observedTimestampEpochNanos, logRecordData.getObservedTimestampEpochNanos());

        final long threadId = Thread.currentThread().getId();
        final String threadName = Thread.currentThread().getName();

        assertEquals(threadId, ((NRLogRecord.BasicLogRecordData) logRecordData).getThreadId());
        assertEquals(threadName, ((NRLogRecord.BasicLogRecordData) logRecordData).getThreadName());
    }

    private static Map<String, Object> getAttributesMap() {
        Map<String, Object> attributesMap = new HashMap<String, Object>() {{
            put("service.name", "test-service");
            put("service.version", "1.0.0");
            put("environment", "production");
        }};

        try {
            throw new RuntimeException("This is a test exception for severity ERROR");
        } catch (RuntimeException e) {
            exceptionMessage = e.getMessage();
            exceptionType = e.getClass().toString();
            exceptionStacktrace = Arrays.toString(e.getStackTrace());
            attributesMap.put("exception.message", exceptionMessage);
            attributesMap.put("exception.type", exceptionType);
            attributesMap.put("exception.stacktrace", exceptionStacktrace);
        }
        return attributesMap;
    }
}