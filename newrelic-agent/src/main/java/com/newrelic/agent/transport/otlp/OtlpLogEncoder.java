/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.model.LogEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

    // ExportLogsServiceRequest
    private static final int REQUEST_RESOURCE_LOGS = 1;

    // ResourceLogs
    private static final int RESOURCE_LOGS_RESOURCE = 1;
    private static final int RESOURCE_LOGS_SCOPE_LOGS = 2;

    // ScopeLogs
    private static final int SCOPE_LOGS_SCOPE = 1;
    private static final int SCOPE_LOGS_LOG_RECORDS = 2;

    // LogRecord
    private static final int LOG_RECORD_TIME_UNIX_NANO = 1;
    private static final int LOG_RECORD_SEVERITY_NUMBER = 2;
    private static final int LOG_RECORD_SEVERITY_TEXT = 3;
    private static final int LOG_RECORD_BODY = 5;
    private static final int LOG_RECORD_ATTRIBUTES = 6;
    private static final int LOG_RECORD_TRACE_ID = 9;
    private static final int LOG_RECORD_SPAN_ID = 10;
    private static final int LOG_RECORD_OBSERVED_TIME_UNIX_NANO = 11;

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

    private static final Set<String> NATIVE_FIELD_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            MESSAGE, TIMESTAMP, AppLoggingUtils.TRACE_ID, AppLoggingUtils.SPAN_ID)));

    // Timestamps above this value can't be epoch milliseconds (it's the year 33658), so they are already epoch nanoseconds.
    // The OpenTelemetry SDK bridge instrumentation records log timestamps in nanoseconds.
    private static final long MAX_EPOCH_MILLIS = 1_000_000_000_000_000L;

    private static final int ESTIMATED_BYTES_PER_LOG = 512;

    private OtlpLogEncoder() {
    }

    static byte[] encode(Map<String, ?> resourceAttributes, Collection<? extends LogEvent> events) {
        ProtobufWriter writer = new ProtobufWriter(events.size() * ESTIMATED_BYTES_PER_LOG);
        writer.startMessage(REQUEST_RESOURCE_LOGS);
        OtlpAttributes.writeResource(writer, RESOURCE_LOGS_RESOURCE, resourceAttributes);
        writer.startMessage(RESOURCE_LOGS_SCOPE_LOGS);
        OtlpAttributes.writeScope(writer, SCOPE_LOGS_SCOPE);
        for (LogEvent event : events) {
            writeLogRecord(writer, event);
        }
        writer.endMessage();
        writer.endMessage();
        return writer.toByteArray();
    }

    private static void writeLogRecord(ProtobufWriter writer, LogEvent event) {
        Map<String, Object> attributes = event.getUserAttributesCopy();
        long observedTimeNanos = TimeUnit.MILLISECONDS.toNanos(event.getTimestamp());

        writer.startMessage(SCOPE_LOGS_LOG_RECORDS);
        writer.writeFixed64(LOG_RECORD_TIME_UNIX_NANO, toEpochNanos(attributes.get(TIMESTAMP), observedTimeNanos));

        Object level = attributes.get(LEVEL);
        if (level != null) {
            String levelText = level.toString();
            int severityNumber = toSeverityNumber(levelText);
            if (severityNumber != SEVERITY_UNSPECIFIED) {
                writer.writeVarint(LOG_RECORD_SEVERITY_NUMBER, severityNumber);
            }
            writer.writeString(LOG_RECORD_SEVERITY_TEXT, levelText);
        }

        Object message = attributes.get(MESSAGE);
        if (message != null) {
            writer.startMessage(LOG_RECORD_BODY);
            OtlpAttributes.writeAnyValue(writer, message);
            writer.endMessage();
        }

        OtlpAttributes.writeAttributes(writer, LOG_RECORD_ATTRIBUTES, attributes, NATIVE_FIELD_ATTRIBUTES);

        byte[] traceId = toIdBytes(attributes.get(AppLoggingUtils.TRACE_ID), OtlpAttributes.TRACE_ID_BYTES);
        if (traceId != null) {
            writer.writeBytes(LOG_RECORD_TRACE_ID, traceId);
        }
        byte[] spanId = toIdBytes(attributes.get(AppLoggingUtils.SPAN_ID), OtlpAttributes.SPAN_ID_BYTES);
        if (spanId != null) {
            writer.writeBytes(LOG_RECORD_SPAN_ID, spanId);
        }

        writer.writeFixed64(LOG_RECORD_OBSERVED_TIME_UNIX_NANO, observedTimeNanos);
        writer.endMessage();
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

    private static byte[] toIdBytes(Object id, int byteLength) {
        return id instanceof String ? OtlpAttributes.hexToBytes((String) id, byteLength) : null;
    }
}
