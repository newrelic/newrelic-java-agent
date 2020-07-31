/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.config.TransactionEventsConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceParent;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * More tests to exercise {@link CrossProcessTransactionStateImpl} can be found in {@link CrossProcessStateTest}
 */
public class CrossProcessTransactionStateImplTest {

    @Mock
    CrossProcessConfig crossProcessConfig;
    @Mock
    Response response;
    @Mock
    Dispatcher dispatcher;
    @Mock
    Transaction tx;
    @Mock
    DistributedTracingConfig distributedTracingConfig;
    DistributedTraceService distributedTracingService;
    SpanEventsServiceImpl spanEventsService;
    @Mock
    SpanEventsConfig spanEventsConfig;
    @Mock
    TransactionEventsConfig transactionEventsConfig;
    @Mock
    AgentConfig agentConfig;
    @Mock
    TransactionActivity ta;
    TransactionStats stats = new TransactionStats();
    MockServiceManager serviceManager = new MockServiceManager();
    private final String accountId = "decafbad";
    private final String applicationId = "donkey";

    @Before
    public void setup() {
        distributedTracingService = mock(DistributedTraceService.class);
        spanEventsService = mock(SpanEventsServiceImpl.class);
        when(distributedTracingService.getAccountId()).thenReturn(accountId);
        when(distributedTracingService.getApplicationId()).thenReturn(applicationId);
        when(spanEventsService.isEnabled()).thenReturn(true);
        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null)));
        serviceManager.setDistributedTraceService(distributedTracingService);
        serviceManager.setSpansEventService(spanEventsService);
        ServiceFactory.setServiceManager(serviceManager);

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void testAppDataHeader() {
        String encodingKey = "test";
        String incomingId = "1#23";
        String txGuid = "5001D";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",1.0,0.2,12345,\"" + txGuid + "\",false]", encodingKey);

        ExtendedRequest request = createRequestFromStandardHeaders(
                Obfuscator.obfuscateNameUsingKey(incomingId, encodingKey),
                null,
                "12345");

        stats = new TransactionStats();

        configureTestMocks(encodingKey, txGuid, obfuscatedAppData, request);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        crossProcessTransactionState.writeResponseHeaders();
        crossProcessTransactionState.writeResponseHeaders();

        verifyMocks(obfuscatedAppData);

        assertEquals(1, stats.getUnscopedStats().getSize());
        ResponseTimeStats clientAppStats = stats.getUnscopedStats().getOrCreateResponseTimeStats("ClientApplication/" + incomingId + "/all");
        assertEquals(1, clientAppStats.getCallCount());

        assertEquals(incomingId, tx.getInboundHeaderState().getClientCrossProcessId());
        assertNull(tx.getInboundHeaderState().getReferrerGuid());
    }

    // The following test used to cover the complex logic that tried to decide when to create and
    // send GUIDs based on the old "Beacon" logic. Eventually the Browser product replaced this with
    // a new and better implementation, so all the Agent support was ripped out sometime after the
    // 3.13.0 release of the Agent. This test was modified to suit; so it compiles and executes now,
    // and passes, but it's unclear what it's really testing.
    @Test
    public void testTransactionHeader() {
        String encodingKey = "test";
        String incomingId = "1#23";
        String transactionHeader = "[\"8cd217491c0264d7\",false]";
        String txGuid = "56b0d429ee4730fe";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",1.0,0.2,12345,\"" + txGuid + "\",false]", encodingKey);

        ExtendedRequest request = createRequestFromStandardHeaders(
                Obfuscator.obfuscateNameUsingKey(incomingId, encodingKey),
                Obfuscator.obfuscateNameUsingKey(transactionHeader, encodingKey),
                "12345");

        configureTestMocks(encodingKey, txGuid, obfuscatedAppData, request);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        crossProcessTransactionState.writeResponseHeaders();
        crossProcessTransactionState.writeResponseHeaders();

        verifyMocks(obfuscatedAppData);

        assertEquals(1, stats.getUnscopedStats().getSize());
        ResponseTimeStats clientAppStats = stats.getUnscopedStats().getOrCreateResponseTimeStats("ClientApplication/" + incomingId + "/all");
        assertEquals(1, clientAppStats.getCallCount());

        assertEquals(incomingId, tx.getInboundHeaderState().getClientCrossProcessId());
    }

    @Test
    public void testGetTransactionHeaderValue() {
        String encodingKey = "test";
        String txGuid = "ee8e5ef1a374c0ec";

        when(crossProcessConfig.isCrossApplicationTracing()).thenReturn(true);
        when(crossProcessConfig.getEncodingKey()).thenReturn(encodingKey);

        when(distributedTracingConfig.isEnabled()).thenReturn(true);

        when(tx.getPriorityTransactionName()).thenReturn(
                PriorityTransactionName.create("Test", "TEST", TransactionNamePriority.NONE)
        );
        when(tx.getApplicationName()).thenReturn("TestApp");
        when(tx.getGuid()).thenReturn(txGuid);
        when(tx.getLock()).thenReturn(new Object());

        mockConfigFromTransaction();

        InboundHeaderState ihs = mock(InboundHeaderState.class);
        when(ihs.getReferrerGuid()).thenReturn(txGuid);
        when(ihs.getInboundTripId()).thenReturn(txGuid);
        when(ihs.getReferringPathHash()).thenReturn(null);
        when(tx.getInboundHeaderState()).thenReturn(ihs);

        TransactionTraceService transactionTraceService = mock(TransactionTraceService.class);

        serviceManager.setTransactionTraceService(transactionTraceService);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);

        String txHeader = "[\"" + txGuid + "\",false,\"" + txGuid + "\",\"8fc87490\"]";
        assertEquals(txHeader, crossProcessTransactionState.getTransactionHeaderValue());
    }

    @Test
    public void testRendersBothHeaders() {
        CrossProcessTransactionStateImpl crossProcessTransactionState = setupTestForDistributedTracing(true);

        OutboundHeadersMap headers = new OutboundHeadersMap(HeaderType.HTTP);
        crossProcessTransactionState.populateRequestMetadata(headers, new OtherRootTracer(
                ta,
                new ClassMethodSignature("my.class", "methodname", "something"),
                "object",
                null
        ));
        assertTrue(headers.containsKey("newrelic"));
        assertTrue(headers.get("newrelic").length() > 0);
        assertTrue(headers.containsKey("traceparent"));
        assertTrue(headers.get("traceparent").length() > 0);
        assertTrue(headers.containsKey("tracestate"));
        assertTrue(headers.get("tracestate").length() > 0);
    }

    @Test
    public void testRendersJustW3CWhenNewRelicHeaderExcluded() {

        CrossProcessTransactionStateImpl crossProcessTransactionState = setupTestForDistributedTracing(false);

        OutboundHeadersMap headers = new OutboundHeadersMap(HeaderType.HTTP);
        crossProcessTransactionState.populateRequestMetadata(headers, new OtherRootTracer(
                ta,
                new ClassMethodSignature("my.class", "methodname", "something"),
                "object",
                null
        ));
        assertFalse(headers.containsKey("newrelic"));
        assertTrue(headers.containsKey("traceparent"));
        assertTrue(headers.get("traceparent").length() > 0);
        assertTrue(headers.containsKey("tracestate"));
        assertTrue(headers.get("tracestate").length() > 0);
    }

    private CrossProcessTransactionStateImpl setupTestForDistributedTracing(boolean includeNewRelicHeader) {
        String encodingKey = "test";
        String txGuid = "ee8e5ef1a374c0ec";

        DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload("14", "15", txGuid, 1.678f);

        when(crossProcessConfig.isCrossApplicationTracing()).thenReturn(true);
        when(crossProcessConfig.getEncodingKey()).thenReturn(encodingKey);

        when(distributedTracingConfig.isEnabled()).thenReturn(true);
        when(distributedTracingConfig.isIncludeNewRelicHeader()).thenReturn(includeNewRelicHeader);

        when(spanEventsConfig.isEnabled()).thenReturn(true);
        when(transactionEventsConfig.isEnabled()).thenReturn(true);
        when(tx.getPriorityTransactionName()).thenReturn(
                PriorityTransactionName.create("Test", "TEST", TransactionNamePriority.NONE)
        );
        when(tx.getApplicationName()).thenReturn("TestApp");
        when(tx.getGuid()).thenReturn(txGuid);
        when(tx.createDistributedTracePayload(anyString())).thenReturn(payload);

        mockConfigFromTransaction();

        InboundHeaderState ihs = mock(InboundHeaderState.class);
        SpanProxy spanProxy = mock(SpanProxy.class);

        when(tx.getInboundHeaderState()).thenReturn(ihs);
        when(tx.sampled()).thenReturn(true);
        when(tx.getMetricAggregator()).thenReturn(mock(MetricAggregator.class));
        when(tx.getSpanProxy()).thenReturn(spanProxy);
        W3CTraceParent traceParent = new W3CTraceParent("1.0", "traceId123abc", "parentId987", 11);
        when(spanProxy.getInitiatingW3CTraceParent()).thenReturn(traceParent);
        when(spanProxy.getOutboundDistributedTracePayload()).thenReturn(payload);

        TransactionTraceService transactionTraceService = mock(TransactionTraceService.class);

        serviceManager.setTransactionTraceService(transactionTraceService);

        return CrossProcessTransactionStateImpl.create(tx);
    }

    @Test
    public void testNoContentLengthNoQueueTime() {
        String encodingKey = "test";
        String incomingId = "1#23";
        String txGuid = "5001D";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",0.0,0.2,-1,\"" + txGuid + "\",false]", encodingKey);

        ExtendedRequest request = createRequestFromStandardHeaders(
                Obfuscator.obfuscateNameUsingKey(incomingId, encodingKey),
                null,
                null
        );

        doNothing().when(response).setHeader("X-NewRelic-App-Data", obfuscatedAppData);

        configureCatEnabled(encodingKey, true);

        mockDispatcher(request);
        mockConfigFromTransaction();
        mockBasicTransactionMethods();

        InboundHeaderState ihs = mock(InboundHeaderState.class);
        when(ihs.getRequestContentLength()).thenReturn(-1L);
        when(ihs.getReferrerGuid()).thenReturn(null);
        when(ihs.isTrustedCatRequest()).thenReturn(true);
        when(ihs.getClientCrossProcessId()).thenReturn(incomingId);

        when(tx.getInboundHeaderState()).thenReturn(ihs);

        doNothing().when(tx).freezeTransactionName();
        when(tx.getExternalTime()).thenReturn(0L);
        PriorityTransactionName txName = PriorityTransactionName.create("WebTransaction/test/test", null,
                TransactionNamePriority.JSP);
        when(tx.getPriorityTransactionName()).thenReturn(txName);
        when(tx.getTransactionActivity()).thenReturn(ta);

        doNothing().when(ta).markAsResponseSender();

        when(ta.getTransactionStats()).thenReturn(stats);
        long durationInNanos = TimeUnit.NANOSECONDS.convert(200L, TimeUnit.MILLISECONDS);
        when(tx.getRunningDurationInNanos()).thenReturn(durationInNanos);
        when(tx.getGuid()).thenReturn(txGuid);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        crossProcessTransactionState.writeResponseHeaders();

        verifyMocks(obfuscatedAppData);

        assertEquals(1, stats.getUnscopedStats().getSize());
        ResponseTimeStats clientAppStats = stats.getUnscopedStats().getOrCreateResponseTimeStats("ClientApplication/" + incomingId + "/all");
        assertEquals(1, clientAppStats.getCallCount());
        assertEquals(incomingId, tx.getInboundHeaderState().getClientCrossProcessId());
    }

    @Test
    public void testUntrustedAccountId() {
        String encodingKey = "test";

        // Set up enough mocks that we can create a real InboundHeaderState instance

        ExtendedRequest request = createRequestFromStandardHeaders(null, null, null);

        configureCatEnabled(encodingKey, false);

        mockDispatcher(request);

        mockBasicTransactionMethods();

        mockConfigFromTransaction();

        when(tx.acceptDistributedTracePayload(anyString())).thenReturn(true);

        InboundHeaderState ihs = new InboundHeaderState(tx, request);
        when(tx.getInboundHeaderState()).thenReturn(ihs);

        when(tx.getTransactionActivity()).thenReturn(ta);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        crossProcessTransactionState.writeResponseHeaders();

        verify(response, never()).setHeader(anyString(), anyString());

        assertNull(tx.getInboundHeaderState().getClientCrossProcessId());
    }

    @Test
    public void testBlankHeader() {
        String encodingKey = "test";

        ExtendedRequest request = createRequestFromStandardHeaders(null, null, null);

        configureCatEnabled(encodingKey, null);

        mockDispatcher(request);

        mockConfigFromTransaction();

        mockBasicTransactionMethods();

        when(tx.acceptDistributedTracePayload(anyString())).thenReturn(true);

        InboundHeaderState ihs = new InboundHeaderState(tx, request);
        when(tx.getInboundHeaderState()).thenReturn(ihs);

        when(tx.getTransactionActivity()).thenReturn(ta);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        crossProcessTransactionState.writeResponseHeaders();

        verify(response, never()).setHeader(anyString(), anyString());

        assertNull(tx.getInboundHeaderState().getClientCrossProcessId());
    }

    @Test
    public void testAlternatePathHashes() {
        InboundHeaderState ihs = mock(InboundHeaderState.class);
        when(ihs.getReferringPathHash()).thenReturn(0);

        when(tx.getInboundHeaderState()).thenReturn(ihs);
        when(tx.getApplicationName()).thenReturn("TestApp");
        when(tx.getPriorityTransactionName()).thenReturn(mock(PriorityTransactionName.class));
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test1");
        when(tx.getLock()).thenReturn(new Object());

        when(crossProcessConfig.getEncodingKey()).thenReturn(null);
        when(crossProcessConfig.getCrossProcessId()).thenReturn(null);
        when(crossProcessConfig.isCrossApplicationTracing()).thenReturn(true);
        when(tx.getCrossProcessConfig()).thenReturn(crossProcessConfig);

        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        assertNull("Should be no alternativePathHashes.", crossProcessTransactionState.getAlternatePathHashes());

        when(tx.getPriorityTransactionName().getName()).thenReturn("Test2");
        assertEquals(ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("TestApp", "Test1", null)),
                crossProcessTransactionState.getAlternatePathHashes());
        assertEquals("e67e0d72", crossProcessTransactionState.getAlternatePathHashes());

        when(tx.getPriorityTransactionName().getName()).thenReturn("Test1");
        assertEquals(ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("TestApp", "Test2", null)),
                crossProcessTransactionState.getAlternatePathHashes());

        when(tx.getPriorityTransactionName().getName()).thenReturn("Test3");
        assertEquals("Should be a sorted list of the first two pathHashes.", "caed9813,e67e0d72",
                crossProcessTransactionState.getAlternatePathHashes());

        when(tx.getPriorityTransactionName().getName()).thenReturn("Test4");
        crossProcessTransactionState.generatePathHash();
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test5");
        assertEquals("Each unique call to generatePathHash should be tracked.",
                "7f945339,b99563b8,caed9813,e67e0d72", crossProcessTransactionState.getAlternatePathHashes());

        when(tx.getPriorityTransactionName().getName()).thenReturn("Test6");
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test7");
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test8");
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test9");
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test10");
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test11");
        crossProcessTransactionState.generatePathHash();
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test12");
        assertEquals("Should only report the first 10, excluding the current one.",
                "08223b61,1001b3a9,3660cb2b,7f945339,9cbcaa58,b99563b8,c856855a,caed9813,e224785c,e67e0d72",
                crossProcessTransactionState.getAlternatePathHashes());
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test1");
        assertEquals("Should only report the first 10, excluding the current one.",
                "08223b61,1001b3a9,3660cb2b,7f945339,9cbcaa58,b99563b8,c856855a,caed9813,e224785c",
                crossProcessTransactionState.getAlternatePathHashes());
        when(tx.getPriorityTransactionName().getName()).thenReturn("Test2");
        assertEquals("Should only report the first 10, excluding the current one.",
                "08223b61,1001b3a9,3660cb2b,7f945339,9cbcaa58,b99563b8,c856855a,e224785c,e67e0d72",
                crossProcessTransactionState.getAlternatePathHashes());
    }

    @Test
    public void testCrossProcessTracingDisabled() {
        ExtendedRequest request = createRequestFromStandardHeaders(null, null, null);

        when(crossProcessConfig.getEncodingKey()).thenReturn(null);
        when(crossProcessConfig.getCrossProcessId()).thenReturn(null);
        when(crossProcessConfig.isCrossApplicationTracing()).thenReturn(false);
        when(distributedTracingConfig.isEnabled()).thenReturn(false);

        mockDispatcher(request);
        mockConfigFromTransaction();
        mockBasicTransactionMethods();

        when(tx.acceptDistributedTracePayload(anyString())).thenReturn(true);

        InboundHeaderState ihs = new InboundHeaderState(tx, request);

        assertNull(ihs.getClientCrossProcessId());
    }

    private void configureTestMocks(String encodingKey, String txGuid, String obfuscatedAppData, ExtendedRequest request) {
        configureCatEnabled(encodingKey, true);

        doNothing().when(response).setHeader("X-NewRelic-App-Data", obfuscatedAppData);
        mockDispatcher(request);

        mockBasicTransactionMethods();

        mockConfigFromTransaction();

        when(tx.acceptDistributedTracePayload(anyString())).thenReturn(true);

        InboundHeaderState ihs = new InboundHeaderState(tx, new DeobfuscatedInboundHeaders(request,
                crossProcessConfig.getEncodingKey()));

        when(tx.getInboundHeaderState()).thenReturn(ihs);

        doNothing().when(tx).freezeTransactionName();
        when(tx.getExternalTime()).thenReturn(1000L);
        PriorityTransactionName txName = PriorityTransactionName.create("WebTransaction/test/test", null,
                TransactionNamePriority.JSP);

        when(tx.getPriorityTransactionName()).thenReturn(txName);
        when(tx.getTransactionActivity()).thenReturn(ta);

        doNothing().when(ta).markAsResponseSender();

        when(ta.getTransactionStats()).thenReturn(stats);
        long durationInNanos = TimeUnit.NANOSECONDS.convert(200L, TimeUnit.MILLISECONDS);
        when(tx.getRunningDurationInNanos()).thenReturn(durationInNanos);
        when(tx.getGuid()).thenReturn(txGuid);
    }

    private void mockConfigFromTransaction() {
        when(agentConfig.getDistributedTracingConfig()).thenReturn(distributedTracingConfig);
        when(tx.getAgentConfig()).thenReturn(agentConfig);
        when(tx.getCrossProcessConfig()).thenReturn(crossProcessConfig);

        when(agentConfig.getSpanEventsConfig()).thenReturn(spanEventsConfig);
        when(agentConfig.getTransactionEventsConfig()).thenReturn(transactionEventsConfig);
    }

    private void mockDispatcher(ExtendedRequest request) {
        when(response.getHeaderType()).thenReturn(HeaderType.HTTP);

        when(dispatcher.isWebTransaction()).thenReturn(true);
        when(dispatcher.getRequest()).thenReturn(request);
        when(dispatcher.getResponse()).thenReturn(response);
        when(tx.getDispatcher()).thenReturn(dispatcher);
    }

    private void configureCatEnabled(String encodingKey, Boolean trustsAccount1) {
        when(crossProcessConfig.isCrossApplicationTracing()).thenReturn(true);
        when(crossProcessConfig.getEncodingKey()).thenReturn(encodingKey);
        when(crossProcessConfig.getCrossProcessId()).thenReturn("6#66");
        if (trustsAccount1 != null) {
            when(crossProcessConfig.isTrustedAccountId("1")).thenReturn(trustsAccount1);
        }

        when(distributedTracingConfig.isEnabled()).thenReturn(false);
    }

    private void mockBasicTransactionMethods() {
        when(tx.getLock()).thenReturn(new Object());
        when(tx.getSpanProxy()).thenReturn(new SpanProxy());
        when(tx.isIgnore()).thenReturn(false);
    }

    private void verifyMocks(String obfuscatedAppData) {
        verify(distributedTracingConfig, atLeastOnce()).isEnabled();
        verify(ta, atLeastOnce()).markAsResponseSender();
        verify(response).setHeader("X-NewRelic-App-Data", obfuscatedAppData);
        verify(tx, atLeastOnce()).freezeTransactionName();
        verify(tx, times(1)).getRunningDurationInNanos();
    }

    private ExtendedRequest createRequestFromStandardHeaders(String newrelicId, String transactionId, String contentLength) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-NewRelic-ID", newrelicId);
        headers.put("X-NewRelic-Transaction", transactionId);
        headers.put("X-NewRelic-Synthetics", null);
        headers.put("Content-Length", contentLength);
        return createRequest(headers);
    }

    private ExtendedRequest createRequest(final Map<String, String> values) {
        return new ExtendedRequest() {
            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public Enumeration getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String name) {
                return new String[0];
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public String getCookieValue(String name) {
                return null;
            }

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public String getHeader(String name) {
                return values.get(name);
            }
        };
    }

}
