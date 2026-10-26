/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.model.LogEvent;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Encodes {@link LogEvent}s as an OTLP {@code ExportLogsServiceRequest} (see opentelemetry/proto/logs/v1/logs.proto).
 * <p>
 * Attributes keep their New Relic names so that logs look the same in New Relic as when they are sent to the collector.
 * Only the attributes that map onto native {@code LogRecord} fields are removed from the attribute list.
 */
final class OtlpLogEncoder {

    // SeverityNumber
    static final int SEVERITY_UNSPECIFIED = 0;
    static final int SEVERITY_TRACE = 1;
    static final int SEVERITY_DEBUG = 5;
    static final int SEVERITY_INFO = 9;
    static final int SEVERITY_WARN = 13;
    static final int SEVERITY_ERROR = 17;
    static final int SEVERITY_FATAL = 21;

    private static final String MESSAGE = AppLoggingUtils.MESSAGE.getKey();
    private static final String TIMESTAMP = AppLoggingUtils.TIMESTAMP.getKey();
    private static final String LEVEL = AppLoggingUtils.LEVEL.getKey();

    static final String EVENT_NAME = "event.name";

    // LogRecord.event_name (string, field 12) was added in opentelemetry-proto 1.5.0, but newer releases require protobuf-java 4.x.
    // The pinned 1.0.0-alpha classes don't have it, so it is written as an unknown field, which serializes identically.
    static final int EVENT_NAME_FIELD_NUMBER = 12;

    private static final Set<String> NATIVE_FIELD_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            MESSAGE, TIMESTAMP, AppLoggingUtils.TRACE_ID, AppLoggingUtils.SPAN_ID, EVENT_NAME,
            OtlpAttributes.OTEL_LIBRARY_NAME, OtlpAttributes.OTEL_LIBRARY_VERSION)));

    // Timestamps above this value can't be epoch milliseconds (it's the year 33658), so they are already epoch nanoseconds.
    // The OpenTelemetry SDK bridge instrumentation records log timestamps in nanoseconds.
    private static final long MAX_EPOCH_MILLIS = 1_000_000_000_000_000L;

    private OtlpLogEncoder() {
    }

    static byte[] encode(Map<String, ?> resourceAttributes, Collection<? extends LogEvent> events) {
        // Events carrying otel.library.name/version keep that scope; all others get the agent's scope
        Map<List<String>, ScopeLogs.Builder> scopes = new LinkedHashMap<>();
        for (LogEvent event : events) {
            Map<String, Object> attributes = event.getUserAttributesCopy();
            List<String> scopeKey = Arrays.asList(
                    asString(attributes.get(OtlpAttributes.OTEL_LIBRARY_NAME)),
                    asString(attributes.get(OtlpAttributes.OTEL_LIBRARY_VERSION)));
            ScopeLogs.Builder scopeLogs = scopes.get(scopeKey);
            if (scopeLogs == null) {
                scopeLogs = ScopeLogs.newBuilder().setScope(OtlpAttributes.buildScope(scopeKey.get(0), scopeKey.get(1)));
                scopes.put(scopeKey, scopeLogs);
            }
            scopeLogs.addLogRecords(buildLogRecord(event, attributes));
        }
        ResourceLogs.Builder resourceLogs = ResourceLogs.newBuilder().setResource(OtlpAttributes.buildResource(resourceAttributes));
        for (ScopeLogs.Builder scopeLogs : scopes.values()) {
            resourceLogs.addScopeLogs(scopeLogs);
        }
        return ExportLogsServiceRequest.newBuilder().addResourceLogs(resourceLogs).build().toByteArray();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static LogRecord buildLogRecord(LogEvent event, Map<String, Object> attributes) {
        long observedTimeNanos = TimeUnit.MILLISECONDS.toNanos(event.getTimestamp());

        LogRecord.Builder builder = LogRecord.newBuilder()
                .setTimeUnixNano(toEpochNanos(attributes.get(TIMESTAMP), observedTimeNanos))
                .setObservedTimeUnixNano(observedTimeNanos);

        Object level = attributes.get(LEVEL);
        if (level != null) {
            String levelText = level.toString();
            int severityNumber = toSeverityNumber(levelText);
            if (severityNumber != SEVERITY_UNSPECIFIED) {
                builder.setSeverityNumberValue(severityNumber);
            }
            builder.setSeverityText(levelText);
        }

        Object message = attributes.get(MESSAGE);
        if (message != null) {
            builder.setBody(OtlpAttributes.buildAnyValue(message));
        }

        builder.addAllAttributes(OtlpAttributes.buildAttributes(attributes, NATIVE_FIELD_ATTRIBUTES));

        ByteString traceId = toIdBytes(attributes.get(AppLoggingUtils.TRACE_ID), OtlpAttributes.TRACE_ID_BYTES);
        if (traceId != null) {
            builder.setTraceId(traceId);
        }
        ByteString spanId = toIdBytes(attributes.get(AppLoggingUtils.SPAN_ID), OtlpAttributes.SPAN_ID_BYTES);
        if (spanId != null) {
            builder.setSpanId(spanId);
        }

        Object eventName = attributes.get(EVENT_NAME);
        if (eventName != null && !eventName.toString().isEmpty()) {
            builder.setUnknownFields(UnknownFieldSet.newBuilder()
                    .addField(EVENT_NAME_FIELD_NUMBER, UnknownFieldSet.Field.newBuilder()
                            .addLengthDelimited(ByteString.copyFromUtf8(eventName.toString()))
                            .build())
                    .build());
        }

        return builder.build();
    }

    static long toEpochNanos(Object timestamp, long defaultNanos) {
        if (!(timestamp instanceof Number)) {
            return defaultNanos;
        }
        long value = ((Number) timestamp).longValue();
        if (value <= 0) {
            return defaultNanos;
        }
        return value > MAX_EPOCH_MILLIS ? value : TimeUnit.MILLISECONDS.toNanos(value);
    }

    /**
     * Maps log levels from the logging frameworks the agent instruments (log4j, logback, JUL, JBoss logging) to OTLP severity numbers.
     */
    static int toSeverityNumber(String level) {
        switch (level.toUpperCase(Locale.ROOT)) {
            case "TRACE":
            case "FINEST":
            case "FINER":
                return SEVERITY_TRACE;
            case "DEBUG":
            case "FINE":
            case "CONFIG":
                return SEVERITY_DEBUG;
            case "INFO":
                return SEVERITY_INFO;
            case "WARN":
            case "WARNING":
                return SEVERITY_WARN;
            case "ERROR":
            case "SEVERE":
                return SEVERITY_ERROR;
            case "FATAL":
                return SEVERITY_FATAL;
            default:
                return SEVERITY_UNSPECIFIED;
        }
    }

    private static ByteString toIdBytes(Object id, int byteLength) {
        if (!(id instanceof String)) {
            return null;
        }
        byte[] bytes = OtlpAttributes.hexToBytes((String) id, byteLength);
        return bytes == null ? null : ByteString.copyFrom(bytes);
    }
}
