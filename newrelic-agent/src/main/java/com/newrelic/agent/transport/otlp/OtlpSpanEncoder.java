/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.model.EventOnSpan;
import com.newrelic.agent.model.LinkOnSpan;
import com.newrelic.agent.model.SpanEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Encodes {@link SpanEvent}s as an OTLP {@code ExportTraceServiceRequest} (see opentelemetry/proto/trace/v1/trace.proto).
 * <p>
 * Intrinsic, agent and user attributes keep their New Relic names so that spans look the same in New Relic as when they
 * are sent to the collector. Only the intrinsics that map onto native {@code Span} fields are removed from the attribute list.
 */
final class OtlpSpanEncoder {

    // ExportTraceServiceRequest
    private static final int REQUEST_RESOURCE_SPANS = 1;

    // ResourceSpans
    private static final int RESOURCE_SPANS_RESOURCE = 1;
    private static final int RESOURCE_SPANS_SCOPE_SPANS = 2;

    // ScopeSpans
    private static final int SCOPE_SPANS_SCOPE = 1;
    private static final int SCOPE_SPANS_SPANS = 2;

    // Span
    private static final int SPAN_TRACE_ID = 1;
    private static final int SPAN_SPAN_ID = 2;
    private static final int SPAN_PARENT_SPAN_ID = 4;
    private static final int SPAN_NAME = 5;
    private static final int SPAN_KIND = 6;
    private static final int SPAN_START_TIME_UNIX_NANO = 7;
    private static final int SPAN_END_TIME_UNIX_NANO = 8;
    private static final int SPAN_ATTRIBUTES = 9;
    private static final int SPAN_EVENTS = 11;
    private static final int SPAN_LINKS = 13;
    private static final int SPAN_STATUS = 15;

    // Span.Event
    private static final int EVENT_TIME_UNIX_NANO = 1;
    private static final int EVENT_NAME = 2;
    private static final int EVENT_ATTRIBUTES = 3;

    // Span.Link
    private static final int LINK_TRACE_ID = 1;
    private static final int LINK_SPAN_ID = 2;
    private static final int LINK_ATTRIBUTES = 4;

    // Status
    private static final int STATUS_MESSAGE = 2;
    private static final int STATUS_CODE = 3;
    static final int STATUS_CODE_ERROR = 2;

    // SpanKind
    static final int SPAN_KIND_INTERNAL = 1;
    static final int SPAN_KIND_SERVER = 2;
    static final int SPAN_KIND_CLIENT = 3;
    static final int SPAN_KIND_PRODUCER = 4;
    static final int SPAN_KIND_CONSUMER = 5;

    private static final String TRACE_ID = "traceId";
    private static final String GUID = "guid";
    private static final String PARENT_ID = "parentId";
    private static final String NAME = "name";
    private static final String TIMESTAMP = "timestamp";
    private static final String DURATION = "duration";
    private static final String TYPE = "type";
    private static final String SPAN_KIND_ATTRIBUTE = "span.kind";
    private static final String ENTRY_POINT = "nr.entryPoint";
    private static final String ERROR_CLASS = "error.class";
    private static final String ERROR_MESSAGE = "error.message";
    private static final String LINKED_TRACE_ID = "linkedTraceId";
    private static final String LINKED_SPAN_ID = "linkedSpanId";

    private static final Set<String> NATIVE_FIELD_INTRINSICS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            TRACE_ID, GUID, PARENT_ID, NAME, TIMESTAMP, DURATION, TYPE)));

    private static final int ESTIMATED_BYTES_PER_SPAN = 1024;

    private OtlpSpanEncoder() {
    }

    static byte[] encode(Map<String, ?> resourceAttributes, Collection<SpanEvent> spans) {
        ProtobufWriter writer = new ProtobufWriter(spans.size() * ESTIMATED_BYTES_PER_SPAN);
        writer.startMessage(REQUEST_RESOURCE_SPANS);
        OtlpAttributes.writeResource(writer, RESOURCE_SPANS_RESOURCE, resourceAttributes);
        writer.startMessage(RESOURCE_SPANS_SCOPE_SPANS);
        OtlpAttributes.writeScope(writer, SCOPE_SPANS_SCOPE);
        for (SpanEvent span : spans) {
            writeSpan(writer, span);
        }
        writer.endMessage();
        writer.endMessage();
        return writer.toByteArray();
    }

    private static void writeSpan(ProtobufWriter writer, SpanEvent span) {
        Map<String, Object> intrinsics = span.getIntrinsics();
        Map<String, Object> agentAttributes = span.getAgentAttributes();

        writer.startMessage(SCOPE_SPANS_SPANS);
        writeId(writer, SPAN_TRACE_ID, intrinsics.get(TRACE_ID), OtlpAttributes.TRACE_ID_BYTES);
        writeId(writer, SPAN_SPAN_ID, intrinsics.get(GUID), OtlpAttributes.SPAN_ID_BYTES);
        writeId(writer, SPAN_PARENT_SPAN_ID, intrinsics.get(PARENT_ID), OtlpAttributes.SPAN_ID_BYTES);

        Object name = intrinsics.get(NAME);
        if (name != null) {
            writer.writeString(SPAN_NAME, name.toString());
        }
        writer.writeVarint(SPAN_KIND, toSpanKind(intrinsics.get(SPAN_KIND_ATTRIBUTE), intrinsics.get(ENTRY_POINT)));

        long startNanos = toNanos(intrinsics.get(TIMESTAMP), span.getTimestamp());
        writer.writeFixed64(SPAN_START_TIME_UNIX_NANO, startNanos);
        writer.writeFixed64(SPAN_END_TIME_UNIX_NANO, startNanos + durationNanos(intrinsics.get(DURATION)));

        OtlpAttributes.writeAttributes(writer, SPAN_ATTRIBUTES, intrinsics, NATIVE_FIELD_INTRINSICS);
        OtlpAttributes.writeAttributes(writer, SPAN_ATTRIBUTES, agentAttributes, Collections.<String>emptySet());
        OtlpAttributes.writeAttributes(writer, SPAN_ATTRIBUTES, span.getUserAttributesCopy(), Collections.<String>emptySet());

        for (EventOnSpan event : span.getEventOnSpanEvents()) {
            writeEvent(writer, event);
        }
        for (LinkOnSpan link : span.getLinkOnSpanEvents()) {
            writeLink(writer, link);
        }

        if (agentAttributes != null && agentAttributes.get(ERROR_CLASS) != null) {
            writer.startMessage(SPAN_STATUS);
            Object errorMessage = agentAttributes.get(ERROR_MESSAGE);
            if (errorMessage != null) {
                writer.writeString(STATUS_MESSAGE, errorMessage.toString());
            }
            writer.writeVarint(STATUS_CODE, STATUS_CODE_ERROR);
            writer.endMessage();
        }
        writer.endMessage();
    }

    private static void writeEvent(ProtobufWriter writer, EventOnSpan event) {
        writer.startMessage(SPAN_EVENTS);
        writer.writeFixed64(EVENT_TIME_UNIX_NANO, TimeUnit.MILLISECONDS.toNanos(event.getTimestamp()));
        if (event.getName() != null) {
            writer.writeString(EVENT_NAME, event.getName());
        }
        OtlpAttributes.writeAttributes(writer, EVENT_ATTRIBUTES, event.getUserAttributesCopy(), Collections.<String>emptySet());
        writer.endMessage();
    }

    private static void writeLink(ProtobufWriter writer, LinkOnSpan link) {
        writer.startMessage(SPAN_LINKS);
        writeId(writer, LINK_TRACE_ID, link.getIntrinsics().get(LINKED_TRACE_ID), OtlpAttributes.TRACE_ID_BYTES);
        writeId(writer, LINK_SPAN_ID, link.getIntrinsics().get(LINKED_SPAN_ID), OtlpAttributes.SPAN_ID_BYTES);
        OtlpAttributes.writeAttributes(writer, LINK_ATTRIBUTES, link.getUserAttributesCopy(), Collections.<String>emptySet());
        writer.endMessage();
    }

    /**
     * The agent only records span.kind for spans that leave the process (client, producer, consumer). Entry point spans are
     * treated as server spans and everything else as internal.
     */
    static int toSpanKind(Object spanKind, Object entryPoint) {
        if (spanKind != null) {
            switch (spanKind.toString()) {
                case "client":
                    return SPAN_KIND_CLIENT;
                case "producer":
                    return SPAN_KIND_PRODUCER;
                case "consumer":
                    return SPAN_KIND_CONSUMER;
                case "server":
                    return SPAN_KIND_SERVER;
                case "internal":
                    return SPAN_KIND_INTERNAL;
                default:
                    break;
            }
        }
        return Boolean.TRUE.equals(entryPoint) ? SPAN_KIND_SERVER : SPAN_KIND_INTERNAL;
    }

    private static long toNanos(Object epochMillis, long defaultEpochMillis) {
        long millis = epochMillis instanceof Number ? ((Number) epochMillis).longValue() : defaultEpochMillis;
        return TimeUnit.MILLISECONDS.toNanos(millis);
    }

    // The agent records span duration in seconds
    private static long durationNanos(Object durationSeconds) {
        if (!(durationSeconds instanceof Number)) {
            return 0;
        }
        return Math.max(0, Math.round(((Number) durationSeconds).doubleValue() * TimeUnit.SECONDS.toNanos(1)));
    }

    private static void writeId(ProtobufWriter writer, int fieldNumber, Object id, int byteLength) {
        if (id instanceof String) {
            byte[] bytes = OtlpAttributes.hexToBytes((String) id, byteLength);
            if (bytes != null) {
                writer.writeBytes(fieldNumber, bytes);
            }
        }
    }
}
