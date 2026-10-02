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
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OtlpSpanEncoderTest {

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String GUID = "b7ad6b7169203331";
    private static final String PARENT_ID = "00f067aa0ba902b7";
    private static final long START_MILLIS = 1_700_000_000_123L;

    @Test
    public void encodesSpanFields() throws Exception {
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put("http.url", "https://example.com/orders");
        agentAttributes.put("http.statusCode", 500);
        agentAttributes.put("error.class", "java.lang.IllegalStateException");
        agentAttributes.put("error.message", "boom");

        SpanEvent span = baseSpan()
                .putIntrinsic("parentId", PARENT_ID)
                .putIntrinsic("category", "http")
                .putIntrinsic("component", "OkHttp")
                .putIntrinsic("sampled", true)
                .putIntrinsic("priority", 1.5f)
                .putIntrinsic("transactionId", "1234567890abcdef")
                .spanKind("client")
                .putAllAgentAttributes(agentAttributes)
                .putAllUserAttributes(Collections.singletonMap("customer", "acme"))
                .build();

        Map<String, Object> resource = Collections.<String, Object>singletonMap("service.name", "My App");
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                OtlpSpanEncoder.encode(resource, Collections.singletonList(span)));

        assertEquals(1, request.getResourceSpansCount());
        ResourceSpans resourceSpans = request.getResourceSpans(0);
        assertEquals(resource, OtlpTestUtil.toMap(resourceSpans.getResource().getAttributesList()));
        assertEquals(OtlpAttributes.SCOPE_NAME, resourceSpans.getScopeSpans(0).getScope().getName());

        Span decoded = resourceSpans.getScopeSpans(0).getSpans(0);
        assertEquals(TRACE_ID, OtlpTestUtil.toHex(decoded.getTraceId()));
        assertEquals(GUID, OtlpTestUtil.toHex(decoded.getSpanId()));
        assertEquals(PARENT_ID, OtlpTestUtil.toHex(decoded.getParentSpanId()));
        assertEquals("External/example.com/OkHttp/execute", decoded.getName());
        assertEquals(OtlpSpanEncoder.SPAN_KIND_CLIENT, decoded.getKindValue());
        assertEquals(1_700_000_000_123_000_000L, decoded.getStartTimeUnixNano());
        assertEquals(1_700_000_000_373_000_000L, decoded.getEndTimeUnixNano());
        assertEquals(Status.StatusCode.STATUS_CODE_ERROR, decoded.getStatus().getCode());
        assertEquals("boom", decoded.getStatus().getMessage());

        Map<String, Object> attributes = OtlpTestUtil.toMap(decoded.getAttributesList());
        assertEquals("http", attributes.get("category"));
        assertEquals("OkHttp", attributes.get("component"));
        assertEquals("client", attributes.get("span.kind"));
        assertEquals(true, attributes.get("sampled"));
        assertEquals(1.5, attributes.get("priority"));
        assertEquals("1234567890abcdef", attributes.get("transactionId"));
        assertEquals("https://example.com/orders", attributes.get("http.url"));
        assertEquals(500L, attributes.get("http.statusCode"));
        assertEquals("java.lang.IllegalStateException", attributes.get("error.class"));
        assertEquals("acme", attributes.get("customer"));
        for (String nativeField : new String[] { "traceId", "guid", "parentId", "name", "timestamp", "duration", "type" }) {
            assertFalse(nativeField + " should be a native span field", attributes.containsKey(nativeField));
        }
    }

    @Test
    public void entryPointSpanIsServerAndOtherSpansAreInternal() throws Exception {
        SpanEvent entrySpan = baseSpan().putIntrinsic("nr.entryPoint", true).build();
        SpanEvent internalSpan = baseSpan().build();

        assertEquals(OtlpSpanEncoder.SPAN_KIND_SERVER, decodeSingle(entrySpan).getKindValue());
        assertEquals(OtlpSpanEncoder.SPAN_KIND_INTERNAL, decodeSingle(internalSpan).getKindValue());
    }

    @Test
    public void spanKindMapping() {
        assertEquals(OtlpSpanEncoder.SPAN_KIND_PRODUCER, OtlpSpanEncoder.toSpanKind("producer", null));
        assertEquals(OtlpSpanEncoder.SPAN_KIND_CONSUMER, OtlpSpanEncoder.toSpanKind("consumer", null));
        assertEquals(OtlpSpanEncoder.SPAN_KIND_SERVER, OtlpSpanEncoder.toSpanKind("server", null));
        assertEquals(OtlpSpanEncoder.SPAN_KIND_INTERNAL, OtlpSpanEncoder.toSpanKind("unexpected", null));
        assertEquals(OtlpSpanEncoder.SPAN_KIND_SERVER, OtlpSpanEncoder.toSpanKind(null, true));
    }

    @Test
    public void spanWithoutErrorHasNoStatus() throws Exception {
        assertFalse(decodeSingle(baseSpan().build()).hasStatus());
    }

    @Test
    public void shortTraceIdIsLeftPaddedAndMissingParentIsOmitted() throws Exception {
        SpanEvent span = baseSpan().putIntrinsic("traceId", "ABCDEF0123456789").build();

        Span decoded = decodeSingle(span);
        assertEquals("0000000000000000abcdef0123456789", OtlpTestUtil.toHex(decoded.getTraceId()));
        assertTrue(decoded.getParentSpanId().isEmpty());
    }

    @Test
    public void missingTimingIntrinsicsFallBackToEventTimestamp() throws Exception {
        SpanEvent span = SpanEvent.builder()
                .putIntrinsic("traceId", TRACE_ID)
                .putIntrinsic("guid", GUID)
                .putIntrinsic("name", "name")
                .timestamp(START_MILLIS)
                .build();

        Span decoded = decodeSingle(span);
        assertEquals(1_700_000_000_123_000_000L, decoded.getStartTimeUnixNano());
        assertEquals(decoded.getStartTimeUnixNano(), decoded.getEndTimeUnixNano());
    }

    @Test
    public void encodesEventsAndLinks() throws Exception {
        EventOnSpan event = EventOnSpan.builder()
                .putIntrinsic("name", "exception")
                .putIntrinsic("span.id", GUID)
                .putIntrinsic("trace.id", TRACE_ID)
                .putAllUserAttributes(Collections.singletonMap("exception.type", "java.io.IOException"))
                .timestamp(START_MILLIS + 10)
                .build();
        LinkOnSpan link = LinkOnSpan.builder()
                .putIntrinsic("id", GUID)
                .putIntrinsic("trace.id", TRACE_ID)
                .putIntrinsic("linkedTraceId", "11111111111111111111111111111111")
                .putIntrinsic("linkedSpanId", "2222222222222222")
                .putAllUserAttributes(Collections.singletonMap("link.reason", "batch"))
                .timestamp(START_MILLIS)
                .build();
        SpanEvent span = baseSpan()
                .eventOnSpanEvents(Collections.singletonList(event))
                .linkOnSpanEvents(Collections.singletonList(link))
                .build();

        Span decoded = decodeSingle(span);

        assertEquals(1, decoded.getEventsCount());
        Span.Event decodedEvent = decoded.getEvents(0);
        assertEquals("exception", decodedEvent.getName());
        assertEquals(1_700_000_000_133_000_000L, decodedEvent.getTimeUnixNano());
        assertEquals(Collections.singletonMap("exception.type", "java.io.IOException"), OtlpTestUtil.toMap(decodedEvent.getAttributesList()));

        assertEquals(1, decoded.getLinksCount());
        Span.Link decodedLink = decoded.getLinks(0);
        assertEquals("11111111111111111111111111111111", OtlpTestUtil.toHex(decodedLink.getTraceId()));
        assertEquals("2222222222222222", OtlpTestUtil.toHex(decodedLink.getSpanId()));
        assertEquals(Collections.singletonMap("link.reason", "batch"), OtlpTestUtil.toMap(decodedLink.getAttributesList()));
    }

    @Test
    public void emptyCollectionProducesValidRequest() throws Exception {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                OtlpSpanEncoder.encode(Collections.<String, Object>emptyMap(), Collections.<SpanEvent>emptyList()));
        assertEquals(0, request.getResourceSpans(0).getScopeSpans(0).getSpansCount());
    }

    private static SpanEvent.Builder baseSpan() {
        return SpanEvent.builder()
                .appName("My App")
                .putIntrinsic("traceId", TRACE_ID)
                .putIntrinsic("guid", GUID)
                .putIntrinsic("name", "External/example.com/OkHttp/execute")
                .putIntrinsic("timestamp", START_MILLIS)
                .putIntrinsic("duration", 0.25f)
                .putIntrinsic("type", "Span");
    }

    private static Span decodeSingle(SpanEvent span) throws Exception {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(
                OtlpSpanEncoder.encode(Collections.<String, Object>emptyMap(), Collections.singletonList(span)));
        return request.getResourceSpans(0).getScopeSpans(0).getSpans(0);
    }
}
