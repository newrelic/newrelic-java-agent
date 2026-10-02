/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.model.LogEvent;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OtlpLogEncoderTest {

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b7ad6b7169203331";

    @Test
    public void encodesLogRecordFields() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("message", "Order 42 shipped");
        attributes.put("timestamp", 1_700_000_000_123L);
        attributes.put("level", "WARN");
        attributes.put("trace.id", TRACE_ID);
        attributes.put("span.id", SPAN_ID);
        attributes.put("entity.guid", "MTIzNDU2fEFQTXxBUFBMSUNBVElPTnwxMjM0");
        attributes.put("entity.name", "My App");
        attributes.put("hostname", "host-1");
        attributes.put("thread.id", 17L);
        attributes.put("thread.name", "main");
        attributes.put("logger.name", "com.example.Orders");
        attributes.put("context.orderId", "42");
        attributes.put("tags.env", "prod");
        LogEvent event = new LogEvent(attributes, 1.0f);

        Map<String, Object> resource = new HashMap<>();
        resource.put("service.name", "My App");
        resource.put("entity.guid", "MTIzNDU2fEFQTXxBUFBMSUNBVElPTnwxMjM0");

        ExportLogsServiceRequest request = ExportLogsServiceRequest.parseFrom(
                OtlpLogEncoder.encode(resource, Collections.singletonList(event)));

        assertEquals(1, request.getResourceLogsCount());
        ResourceLogs resourceLogs = request.getResourceLogs(0);
        assertEquals(resource, OtlpTestUtil.toMap(resourceLogs.getResource().getAttributesList()));
        assertEquals(1, resourceLogs.getScopeLogsCount());
        ScopeLogs scopeLogs = resourceLogs.getScopeLogs(0);
        assertEquals(OtlpAttributes.SCOPE_NAME, scopeLogs.getScope().getName());
        assertEquals(1, scopeLogs.getLogRecordsCount());

        LogRecord record = scopeLogs.getLogRecords(0);
        assertEquals(TimeUnit.MILLISECONDS.toNanos(1_700_000_000_123L), record.getTimeUnixNano());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(event.getTimestamp()), record.getObservedTimeUnixNano());
        assertEquals(OtlpLogEncoder.SEVERITY_WARN, record.getSeverityNumberValue());
        assertEquals("WARN", record.getSeverityText());
        assertEquals("Order 42 shipped", record.getBody().getStringValue());
        assertEquals(TRACE_ID, OtlpTestUtil.toHex(record.getTraceId()));
        assertEquals(SPAN_ID, OtlpTestUtil.toHex(record.getSpanId()));

        Map<String, Object> expectedAttributes = new HashMap<>(attributes);
        expectedAttributes.remove("message");
        expectedAttributes.remove("timestamp");
        expectedAttributes.remove("trace.id");
        expectedAttributes.remove("span.id");
        assertEquals(expectedAttributes, OtlpTestUtil.toMap(record.getAttributesList()));
    }

    @Test
    public void encodesMultipleLogsInOneScope() throws Exception {
        LogEvent first = new LogEvent(Collections.<String, Object>singletonMap("message", "first"), 1.0f);
        LogEvent second = new LogEvent(Collections.<String, Object>singletonMap("message", "second"), 1.0f);

        ExportLogsServiceRequest request = ExportLogsServiceRequest.parseFrom(
                OtlpLogEncoder.encode(Collections.<String, Object>emptyMap(), Arrays.asList(first, second)));

        ScopeLogs scopeLogs = request.getResourceLogs(0).getScopeLogs(0);
        assertEquals(2, scopeLogs.getLogRecordsCount());
        assertEquals("first", scopeLogs.getLogRecords(0).getBody().getStringValue());
        assertEquals("second", scopeLogs.getLogRecords(1).getBody().getStringValue());
    }

    @Test
    public void minimalLogUsesObservedTimeAndOmitsMissingFields() throws Exception {
        LogEvent event = new LogEvent(Collections.<String, Object>emptyMap(), 1.0f);

        LogRecord record = ExportLogsServiceRequest.parseFrom(
                        OtlpLogEncoder.encode(Collections.<String, Object>emptyMap(), Collections.singletonList(event)))
                .getResourceLogs(0).getScopeLogs(0).getLogRecords(0);

        assertEquals(record.getObservedTimeUnixNano(), record.getTimeUnixNano());
        assertFalse(record.hasBody());
        assertEquals("", record.getSeverityText());
        assertEquals(0, record.getSeverityNumberValue());
        assertTrue(record.getTraceId().isEmpty());
        assertTrue(record.getSpanId().isEmpty());
        assertEquals(0, record.getAttributesCount());
    }

    @Test
    public void invalidIdsAreOmittedAndKeptOutOfAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("trace.id", "not-a-trace-id");
        attributes.put("span.id", "");
        LogEvent event = new LogEvent(attributes, 1.0f);

        LogRecord record = ExportLogsServiceRequest.parseFrom(
                        OtlpLogEncoder.encode(Collections.<String, Object>emptyMap(), Collections.singletonList(event)))
                .getResourceLogs(0).getScopeLogs(0).getLogRecords(0);

        assertTrue(record.getTraceId().isEmpty());
        assertTrue(record.getSpanId().isEmpty());
        assertEquals(0, record.getAttributesCount());
    }

    @Test
    public void emptyCollectionProducesValidRequest() throws Exception {
        ExportLogsServiceRequest request = ExportLogsServiceRequest.parseFrom(
                OtlpLogEncoder.encode(Collections.<String, Object>emptyMap(), Collections.<LogEvent>emptyList()));
        assertEquals(0, request.getResourceLogs(0).getScopeLogs(0).getLogRecordsCount());
    }

    @Test
    public void toEpochNanosHandlesMillisNanosAndInvalidValues() {
        assertEquals(1_700_000_000_123_000_000L, OtlpLogEncoder.toEpochNanos(1_700_000_000_123L, 5));
        assertEquals(1_700_000_000_123_456_789L, OtlpLogEncoder.toEpochNanos(1_700_000_000_123_456_789L, 5));
        assertEquals(5, OtlpLogEncoder.toEpochNanos(null, 5));
        assertEquals(5, OtlpLogEncoder.toEpochNanos("1700000000123", 5));
        assertEquals(5, OtlpLogEncoder.toEpochNanos(0L, 5));
    }

    @Test
    public void toSeverityNumberMapsFrameworkLevels() {
        assertEquals(OtlpLogEncoder.SEVERITY_TRACE, OtlpLogEncoder.toSeverityNumber("FINEST"));
        assertEquals(OtlpLogEncoder.SEVERITY_TRACE, OtlpLogEncoder.toSeverityNumber("trace"));
        assertEquals(OtlpLogEncoder.SEVERITY_DEBUG, OtlpLogEncoder.toSeverityNumber("FINE"));
        assertEquals(OtlpLogEncoder.SEVERITY_DEBUG, OtlpLogEncoder.toSeverityNumber("CONFIG"));
        assertEquals(OtlpLogEncoder.SEVERITY_INFO, OtlpLogEncoder.toSeverityNumber("INFO"));
        assertEquals(OtlpLogEncoder.SEVERITY_WARN, OtlpLogEncoder.toSeverityNumber("WARNING"));
        assertEquals(OtlpLogEncoder.SEVERITY_ERROR, OtlpLogEncoder.toSeverityNumber("SEVERE"));
        assertEquals(OtlpLogEncoder.SEVERITY_FATAL, OtlpLogEncoder.toSeverityNumber("FATAL"));
        assertEquals(OtlpLogEncoder.SEVERITY_UNSPECIFIED, OtlpLogEncoder.toSeverityNumber("UNKNOWN"));
    }
}
