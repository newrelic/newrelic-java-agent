/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.agent.environment.AgentIdentity;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ErrorTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceParent;
import com.newrelic.agent.tracing.W3CTraceState;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.agent.util.TimeConversion;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.newrelic.agent.MetricNames.QUEUE_TIME;
import static com.newrelic.agent.attributes.AttributeNames.HTTP_REQUEST_PREFIX;
import static com.newrelic.agent.attributes.AttributeNames.HTTP_STATUS;
import static com.newrelic.agent.attributes.AttributeNames.HTTP_STATUS_MESSAGE;
import static com.newrelic.agent.attributes.AttributeNames.MESSAGE_REQUEST_PREFIX;
import static com.newrelic.agent.attributes.AttributeNames.PORT;
import static com.newrelic.agent.attributes.AttributeNames.QUEUE_DURATION;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_HOST_PARAMETER_NAME;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_METHOD_PARAMETER_NAME;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_REFERER_PARAMETER_NAME;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_URI;
import static com.newrelic.agent.attributes.AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME;
import static com.newrelic.agent.attributes.AttributeNames.RESPONSE_CONTENT_TYPE_PARAMETER_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TracerToSpanEventTest {

    private final String appName = "appName";
    private final String traceId = "12345";
    private boolean isRoot = true;
    private final boolean sampled = true;
    private final long duration = 1500L;
    private final long timestamp = 12345L;
    private final Supplier<Long> timestampProvider = () -> timestamp;
    private final float priority = 1.23F;
    private final int responseStatus = 500;
    private final int port = 8085;
    private final String statusMessage = "yo dawg you got a 500";
    private final PriorityTransactionName txnName = PriorityTransactionName.create("you", "are", "chillnoceros", TransactionNamePriority.CUSTOM_HIGH);
    private Tracer tracer;
    private TransactionData txnData;
    private Map<String, Object> expectedAgentAttributes;
    private Map<String, Object> expectedIntrinsicAttributes;
    private Map<String, Object> expectedUserAttributes;
    private Map<String, SpanErrorBuilder> errorBuilderMap;
    private Map<String, Object> transactionAgentAttributes;
    private Map<String, Object> transactionUserAttributes;
    private Map<String, Object> tracerAgentAttributes;
    private Map<String, Object> tracerUserAttributes;
    private Set<String> tracerAgentAttributeNamesMarkedForSpans;
    private SpanProxy spanProxy;
    private SpanErrorBuilder spanErrorBuilder;
    private TransactionThrowable throwable;
    private SpanError spanError;
    private EnvironmentService environmentService;
    private Environment environment;
    private TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics;
    private TransactionStats txnStats;
    private final int numberOfSpanLinks = 5;

    @Before
    public void setup() {
        transactionAgentAttributes = new HashMap<>();
        transactionUserAttributes = new HashMap<>();
        tracerAgentAttributes = new HashMap<>();
        tracerUserAttributes = new HashMap<>();
        expectedAgentAttributes = new HashMap<>();
        expectedUserAttributes = new HashMap<>();
        tracerAgentAttributeNamesMarkedForSpans = new HashSet<>();
        expectedAgentAttributes.put("error.class", "0");
        expectedAgentAttributes.put("port", 9191);

        expectedIntrinsicAttributes = new HashMap<>();
        expectedIntrinsicAttributes.put("traceId", traceId);
        expectedIntrinsicAttributes.put("duration", (float) duration / TimeConversion.NANOSECONDS_PER_SECOND);
        expectedIntrinsicAttributes.put("type", "Span");
        expectedIntrinsicAttributes.put("category", "generic");
        expectedIntrinsicAttributes.put("sampled", sampled);
        expectedIntrinsicAttributes.put("nr.entryPoint", true);
        expectedIntrinsicAttributes.put("timestamp", timestamp);
        expectedIntrinsicAttributes.put("priority", priority);
        expectedIntrinsicAttributes.put("transaction.name", txnName.getName());

        tracer = mock(Tracer.class);
        txnData = mock(TransactionData.class);
        spanErrorBuilder = mock(SpanErrorBuilder.class);
        spanError = mock(SpanError.class);
        spanProxy = mock(SpanProxy.class);
        throwable = mock(TransactionThrowable.class);
        environmentService = mock(EnvironmentService.class);
        environment = mock(Environment.class);
        transactionDataToDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        txnStats = mock(TransactionStats.class, RETURNS_DEEP_STUBS);

        errorBuilderMap = new HashMap<>();
        errorBuilderMap.put(appName, spanErrorBuilder);

        when(tracer.getDuration()).thenReturn(duration);
        when(tracer.getStartTimeInMillis()).thenReturn(timestamp);
        when(tracer.getAgentAttributes()).thenReturn(tracerAgentAttributes);
        when(tracer.getCustomAttributes()).thenReturn(tracerUserAttributes);
        when(tracer.getAgentAttributeNamesForSpans()).thenReturn(tracerAgentAttributeNamesMarkedForSpans);
        when(tracer.getAgentAttributeNamesForSpans()).thenReturn(tracerAgentAttributeNamesMarkedForSpans);
        when(tracer.getSpanLinks()).thenReturn(createMapOfSpanLinks());
        when(spanErrorBuilder.buildSpanError(tracer, isRoot, responseStatus, statusMessage, throwable)).thenReturn(spanError);
        when(spanErrorBuilder.areErrorsEnabled()).thenReturn(true);
        when(txnData.getApplicationName()).thenReturn(appName);
        when(txnData.getResponseStatus()).thenReturn(responseStatus);
        when(txnData.getStatusMessage()).thenReturn(statusMessage);
        when(txnData.getThrowable()).thenReturn(throwable);
        when(txnData.getPriority()).thenReturn(priority);
        when(txnData.sampled()).thenReturn(sampled);
        when(txnData.getSpanProxy()).thenReturn(spanProxy);
        when(txnData.getPriorityTransactionName()).thenReturn(txnName);
        when(txnData.getUserAttributes()).thenReturn(transactionUserAttributes);

        when(spanProxy.getOrCreateTraceId()).thenReturn(traceId);
        when(environmentService.getEnvironment()).thenReturn(environment);
        when(environment.getAgentIdentity()).thenReturn(new AgentIdentity("dispatcher", "1.2.3", 9191, "myInstance"));
    }

    private List<SpanLink> createMapOfSpanLinks() {
        List<SpanLink> spanLinks = new ArrayList<>();
        for (int i = 0; i < numberOfSpanLinks; i++) {
            String fakeId = String.valueOf(i);
            spanLinks.add(new SpanLink(timestamp, fakeId, fakeId, fakeId, fakeId, Collections.singletonMap("foo", "bar")));
        }
        return spanLinks;
    }

    @Test
    public void testHappyPath() {
        // setup
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testWithTraceState() {
        // setup
        String guid = "1234-abcd-dead-beef";
        expectedIntrinsicAttributes.put("trustedParentId", guid);
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        W3CTraceState traceState = mock(W3CTraceState.class);
        when(traceState.getGuid()).thenReturn(guid);
        when(spanProxy.getInitiatingW3CTraceState()).thenReturn(traceState);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testParentIdFromParentSpan() {
        // setup
        String parentGuid = "98765";
        expectedIntrinsicAttributes.put("parentId", parentGuid);

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        Tracer parentTracer = mock(Tracer.class);

        when(parentTracer.getGuid()).thenReturn(parentGuid);
        when(parentTracer.isTransactionSegment()).thenReturn(true);
        when(spanErrorBuilder.buildSpanError(tracer, false, responseStatus, statusMessage, throwable)).thenReturn(spanError);
        when(tracer.getParentTracer()).thenReturn(parentTracer);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testParentIdFromDTPayload() {
        // setup
        String parentGuid = "98765";
        expectedIntrinsicAttributes.put("parentId", parentGuid);

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        DistributedTracePayloadImpl dtPayload = mock(DistributedTracePayloadImpl.class);

        when(dtPayload.getGuid()).thenReturn(parentGuid);
        when(txnData.getInboundDistributedTracePayload()).thenReturn(dtPayload);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testParentIdFromW3CPayload() {
        // setup
        String parentGuid = "98765";
        expectedIntrinsicAttributes.put("parentId", parentGuid);

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        W3CTraceParent w3cPayload = mock(W3CTraceParent.class);

        when(txnData.getW3CTraceParent()).thenReturn(w3cPayload);
        when(w3cPayload.getParentId()).thenReturn(parentGuid);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testCrossProcessOnly() {
        // setup
        String parentGuid = "98765";

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        Tracer parentTracer = mock(Tracer.class);

        // the parent tracer and guid are used to make sure the test fails if crossProcessOnly isn't set
        when(parentTracer.getGuid()).thenReturn(parentGuid);
        when(parentTracer.isTransactionSegment()).thenReturn(true);
        when(spanErrorBuilder.buildSpanError(tracer, false, responseStatus, statusMessage, throwable)).thenReturn(spanError);
        when(tracer.getParentTracer()).thenReturn(parentTracer);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, true);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testRequestAttributesAddedToRoot() {

        // setup
        String requestUri = "httpss://404.com";
        String requestMethod = "SUPERGET";
        String requestHeadersRefer = "referee";
        String requestHeadersAccept = "college";
        String requestHeadersContentLength = "very long";
        String requestHeadersHost = "Drew Carey";
        String requestHeadersUserAgent = "007";
        float queueDuration = 123456789F;

        expectedAgentAttributes.put(REQUEST_REFERER_PARAMETER_NAME, requestHeadersRefer);
        expectedAgentAttributes.put(REQUEST_ACCEPT_PARAMETER_NAME, requestHeadersAccept);
        expectedAgentAttributes.put(REQUEST_CONTENT_LENGTH_PARAMETER_NAME, requestHeadersContentLength);
        expectedAgentAttributes.put(REQUEST_HOST_PARAMETER_NAME, requestHeadersHost);
        expectedAgentAttributes.put(REQUEST_USER_AGENT_PARAMETER_NAME, requestHeadersUserAgent);
        expectedAgentAttributes.put(REQUEST_METHOD_PARAMETER_NAME, requestMethod);
        expectedAgentAttributes.put(REQUEST_URI, requestUri);
        expectedAgentAttributes.put(PORT, port);
        expectedAgentAttributes.put(QUEUE_DURATION, queueDuration);

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        transactionAgentAttributes.put(REQUEST_REFERER_PARAMETER_NAME, requestHeadersRefer);
        transactionAgentAttributes.put(REQUEST_ACCEPT_PARAMETER_NAME, requestHeadersAccept);
        transactionAgentAttributes.put(REQUEST_CONTENT_LENGTH_PARAMETER_NAME, requestHeadersContentLength);
        transactionAgentAttributes.put(REQUEST_HOST_PARAMETER_NAME, requestHeadersHost);
        transactionAgentAttributes.put(REQUEST_USER_AGENT_PARAMETER_NAME, requestHeadersUserAgent);
        transactionAgentAttributes.put(REQUEST_METHOD_PARAMETER_NAME, requestMethod);
        transactionAgentAttributes.put(REQUEST_URI, requestUri);
        transactionAgentAttributes.put(PORT, port);

        when(txnData.getAgentAttributes()).thenReturn(transactionAgentAttributes);
        when(environment.getAgentIdentity()).thenReturn(new AgentIdentity("dispatcher", "1.2.3", port, "myInstance"));
        when(txnStats.getUnscopedStats().getStatsMap().containsKey(QUEUE_TIME)).thenReturn(true);
        when(txnStats.getUnscopedStats().getOrCreateResponseTimeStats(QUEUE_TIME).getTotal()).thenReturn(queueDuration);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testResponseAttributesAddedToRoot() {

        // setup
        int httpResponseCode = 404;
        String httpResponseMessage = "I cannot find that page, silly";
        String contentType = "application/vnd.ms-powerpoint ";
        expectedAgentAttributes.put("httpResponseCode", httpResponseCode);
        expectedAgentAttributes.put("httpResponseMessage", httpResponseMessage);
        expectedAgentAttributes.put(RESPONSE_CONTENT_TYPE_PARAMETER_NAME, contentType);

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        transactionAgentAttributes.put(HTTP_STATUS, httpResponseCode);
        transactionAgentAttributes.put(HTTP_STATUS_MESSAGE, httpResponseMessage);
        transactionAgentAttributes.put(RESPONSE_CONTENT_TYPE_PARAMETER_NAME, contentType);

        when(txnData.getAgentAttributes()).thenReturn(transactionAgentAttributes);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testPrefixedAttributesAddedToRoot() {

        Map<String, Map<String, String>> prefixedAttributes = new HashMap<>();
        Map<String, String> requestAttributes = new HashMap<>();
        Map<String, String> messagingAttributes = new HashMap<>();

        prefixedAttributes.put(HTTP_REQUEST_PREFIX, requestAttributes);
        prefixedAttributes.put(MESSAGE_REQUEST_PREFIX, messagingAttributes);

        requestAttributes.put("count", "7");
        messagingAttributes.put("messagingService", "fakebook");

        // setup
        expectedAgentAttributes.put(HTTP_REQUEST_PREFIX + "count", "7");
        expectedAgentAttributes.put(MESSAGE_REQUEST_PREFIX + "messagingService", "fakebook");

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        when(txnData.getPrefixedAttributes()).thenReturn(prefixedAttributes);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testDistributedTraceIntrinsicsAreAdded() {
        Map<String, Object> distributedTraceIntrinsics = Collections.singletonMap("dt-intrinsic", "yuppers");

        when(transactionDataToDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), anyBoolean()))
                .thenReturn(distributedTraceIntrinsics);

        expectedAgentAttributes.put("dt-intrinsic", "yuppers");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testDistributedTraceIntrinsicsAreAddedAndFiltered() {
        Map<String, Object> distributedTraceIntrinsics = new HashMap<>();
        distributedTraceIntrinsics.put("dt-intrinsic", "yuppers");
        distributedTraceIntrinsics.put("parentSpanId", "that's a no from me");

        when(transactionDataToDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), anyBoolean()))
                .thenReturn(distributedTraceIntrinsics);

        expectedAgentAttributes.put("dt-intrinsic", "yuppers");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testUserAttributesAreCopied() {

        // setup
        transactionUserAttributes.put("foo", "bar");
        transactionUserAttributes.put("bar", "baz");

        expectedUserAttributes = new HashMap<>();
        expectedUserAttributes.put("foo", "bar");
        expectedUserAttributes.put("bar", "baz");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testIntrinsicTransactionAttrsCopiedToSpan() {
        // setup
        Map<String, Object> intrinsicAttributes = new HashMap<>();
        intrinsicAttributes.put("hot", "sauce");
        intrinsicAttributes.put("saucy", "burrito");

        expectedAgentAttributes.put("hot", "sauce");
        expectedAgentAttributes.put("saucy", "burrito");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        when(txnData.getIntrinsicAttributes()).thenReturn(intrinsicAttributes);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testUndesiredAttributesFiltered() {
        // setup
        Map<String, Object> intrinsicAttributes = new HashMap<>();
        intrinsicAttributes.put("foo", "bar");
        intrinsicAttributes.put("externalCallCount", 13);

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("ok", "okey");
        userAttributes.put("host.displayName", "myrad.server");
        userAttributes.put("parentSpanId", "1234-abcd");

        transactionAgentAttributes.put("a", "b");
        transactionAgentAttributes.put("nr.guid", "I sure hope I don't make it to the output");

        expectedAgentAttributes.put("a", "b");
        expectedAgentAttributes.put("foo", "bar");
        expectedUserAttributes = new HashMap<>();
        expectedUserAttributes.put("ok", "okey");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        when(txnData.getIntrinsicAttributes()).thenReturn(intrinsicAttributes);
        when(txnData.getUserAttributes()).thenReturn(userAttributes);
        when(txnData.getAgentAttributes()).thenReturn(transactionAgentAttributes);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testAgentAttributesMarkedForSpansAdded() {
        // set up

        tracerAgentAttributes.put("key1", "v1");
        tracerAgentAttributes.put("key2", "v2");

        tracerAgentAttributeNamesMarkedForSpans.add("key1");
        tracerAgentAttributeNamesMarkedForSpans.add("key3");

        when(txnData.getAgentAttributes()).thenReturn(transactionAgentAttributes);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals("v1", spanEvent.getAgentAttributes().get("key1"));
        assertNull(spanEvent.getAgentAttributes().get("key2"));
        assertNull(spanEvent.getAgentAttributes().get("key3"));
    }

    @Test
    public void testErrorCollectorDisabled() {
        // setup
        expectedAgentAttributes.remove("error.class");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();
        when(spanErrorBuilder.areErrorsEnabled()).thenReturn(false);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testAutoAppNaming() {
        // setup
        String newAppName = "new app name";
        when(txnData.getApplicationName()).thenReturn(newAppName);
        SpanEvent expectedSpanEvent = SpanEvent.builder()
                .appName(newAppName)
                .priority(priority)
                .putAllAgentAttributes(expectedAgentAttributes)
                .putAllIntrinsics(expectedIntrinsicAttributes)
                .putAllUserAttributes(expectedUserAttributes)
                .timestamp(timestamp)
                .build();
        when(spanErrorBuilder.areErrorsEnabled()).thenReturn(true);

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testTracerAttributes() {
        // setup
        tracerUserAttributes.put("user", "attribute");
        expectedUserAttributes.put("user", "attribute");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testTracerCustomAttributeOverlap() {
        // setup
        transactionUserAttributes.put("testAttrib1", "txValue");
        tracerUserAttributes.put("testAttrib1", "spanValue");
        expectedUserAttributes.put("testAttrib1", "spanValue");

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testAttributeLimits() {
        // setup
        for (int i = 1; i <= 65; i++) {
            transactionUserAttributes.put("TxAttrib" + i, "TxValue" + i);
        }
        for (int i = 1; i <= 30; i++) {
            tracerUserAttributes.put("SpanAttrib" + i, "SpanValue" + i);
        }
        expectedUserAttributes.putAll(tracerUserAttributes);

        for (int i = 1; i <= 34; i++) {
            expectedUserAttributes.put("TxAttrib" + i, "TxValue" + 1);
        }

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals("size was actually " + spanEvent.getUserAttributesCopy().size(), 64, spanEvent.getUserAttributesCopy().size());

        int spanAttCount = 0;
        for (String key : spanEvent.getUserAttributesCopy().keySet()) {
            if (key.startsWith("SpanAttrib")) {
                spanAttCount++;
            }
        }
        assertEquals(30, spanAttCount);
    }

    @Test
    public void testSpanAttributeLimits() {
        // setup
        for (int i = 1; i <= 100; i++) {
            tracerUserAttributes.put("SpanAttrib" + i, "SpanValue" + i);
        }

        for (int i = 1; i <= 64; i++) {
            expectedUserAttributes.put("SpanAttrib" + i, "SpanValue" + i);
        }

        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        // assertions
        assertEquals("size was actually " + spanEvent.getUserAttributesCopy().size(), 64, spanEvent.getUserAttributesCopy().size());
    }

    @Test
    public void testGraphQLAttribute() {
        // setup
        expectedAgentAttributes.put("graphql.operation.type", "Query");
        expectedAgentAttributes.put("graphql.field.name", "book");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        tracerAgentAttributes.put("graphql.operation.type", "Query");
        tracerAgentAttributes.put("graphql.field.name", "book");

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        // execution
        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, true, false);

        when(txnData.getAgentAttributes()).thenReturn(transactionAgentAttributes);
        // assertions
        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testClmAttributes() {
        isRoot = false;
        expectedAgentAttributes.put(AttributeNames.CLM_NAMESPACE, "className");
        expectedAgentAttributes.put(AttributeNames.CLM_FUNCTION, "method");
        // these attrs make sense for a root span, but not to a non-root span
        expectedAgentAttributes.remove("port");
        expectedAgentAttributes.remove("error.class");
        expectedIntrinsicAttributes.remove("nr.entryPoint");
        expectedIntrinsicAttributes.remove("transaction.name");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        tracerAgentAttributes.put(AttributeNames.CLM_NAMESPACE, "className");
        tracerAgentAttributes.put(AttributeNames.CLM_FUNCTION, "method");
        tracerAgentAttributes.put("randomAttribute", "some value"); // only the CLM attrs should be in the spanEvent

        when(spanErrorBuilder.buildSpanError(any(ErrorTracer.class), eq(false), anyInt(), anyString(), any(TransactionThrowable.class)))
                .thenReturn(new SpanError());

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testClmAttributesOnRootSpan() {
        expectedAgentAttributes.put(AttributeNames.CLM_NAMESPACE, "className");
        expectedAgentAttributes.put(AttributeNames.CLM_FUNCTION, "method");
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();

        tracerAgentAttributes.put(AttributeNames.CLM_NAMESPACE, "className");
        tracerAgentAttributes.put(AttributeNames.CLM_FUNCTION, "method");
        tracerAgentAttributes.put("randomAttribute", "some value"); // only the CLM attrs should be in the spanEvent

        when(spanErrorBuilder.buildSpanError(any(ErrorTracer.class), eq(false), anyInt(), anyString(), any(TransactionThrowable.class)))
                .thenReturn(new SpanError());

        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        assertEquals(expectedSpanEvent, spanEvent);
    }

    @Test
    public void testCreateLinkOnSpanEvents() {
        SpanEvent expectedSpanEvent = buildExpectedSpanEvent();
        TracerToSpanEvent testClass = new TracerToSpanEvent(errorBuilderMap, new AttributeFilter.PassEverythingAttributeFilter(), timestampProvider,
                environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);

        SpanEvent spanEvent = testClass.createSpanEvent(tracer, txnData, txnStats, isRoot, false);

        assertEquals(expectedSpanEvent, spanEvent);
        assertEquals(numberOfSpanLinks, spanEvent.getLinkOnSpanEvents().size());
    }

    private SpanEvent buildExpectedSpanEvent() {
        return SpanEvent.builder()
                .appName(appName)
                .priority(priority)
                .putAllAgentAttributes(expectedAgentAttributes)
                .putAllIntrinsics(expectedIntrinsicAttributes)
                .putAllUserAttributes(expectedUserAttributes)
                .timestamp(timestamp)
                .build();
    }
}
