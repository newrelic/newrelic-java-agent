package com.newrelic.agent.tracing;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.AgentImpl;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.config.AgentConfigImpl.APP_NAME;
import static com.newrelic.agent.config.DistributedTracingConfig.ACCOUNT_ID;
import static com.newrelic.agent.config.DistributedTracingConfig.PRIMARY_APPLICATION_ID;
import static com.newrelic.agent.config.DistributedTracingConfig.TRUSTED_ACCOUNT_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DistributedTracingApiTest {

    private MockServiceManager serviceManager;
    private AgentConfig agentConfig;

    private Transaction transaction;
    private Tracer rootTracer;

    @Before
    public void setup() throws Exception {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        setupConfig(false);

        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setAttributesService(new AttributesService());
        AgentBridge.agent = new AgentImpl(Agent.LOG);

        serviceManager.setRPMServiceManager(new MockRPMServiceManager());
        ServiceFactory.getServiceManager().start();

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        ServiceFactory.getTransactionService().addTransactionListener(distributedTraceService);
        SpanEventsServiceImpl spanEventsService = SpanEventsServiceImpl.builder().agentConfig(agentConfig).build();
        serviceManager.setDistributedTraceService(distributedTraceService);
        serviceManager.setSpansEventService(spanEventsService);

        Map<String, Object> connectInfo = ImmutableMap.<String, Object>of(ACCOUNT_ID, "33", TRUSTED_ACCOUNT_KEY, "33", PRIMARY_APPLICATION_ID, "2827902");
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        distributedTraceService.connected(null, agentConfig);
    }

    @After
    public void teardown() {
        if (rootTracer != null) {
            rootTracer.finish(Opcodes.RETURN, 0);
        }
    }

    private void setupConfig(boolean excludeNRHeader) {
        Map<String, Object> dtConfig = ImmutableMap.<String, Object>of(
                "enabled", true,
                "exclude_newrelic_header", excludeNRHeader);
        Map<String, Object> spanConfig = ImmutableMap.of("collect_span_events", (Object) true);
        Map<String, Object> config = ImmutableMap.of(
                APP_NAME, "test",
                "distributed_tracing", dtConfig,
                "span_events", spanConfig);

        agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
    }

    private void setupTransactionAndTracer() {
        transaction = Transaction.getTransaction();
        rootTracer = TransactionAsyncUtility.createAndStartDispatcherTracer(this, "WebTransaction", new MockHttpRequest());
        transaction.getTransactionActivity().tracerStarted(rootTracer);
    }

    @Test
    public void testAccept() {
        setupTransactionAndTracer();

        // make API call
        Headers headers = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
        headers.setHeader("traceparent", "00-da8bc8cc6d062849b0efcf3c169afb5a-7d3efb1b173fecfa-01");
        headers.setHeader("tracestate", "33@nr=0-0-33-2827902-7d3efb1b173fecfa-e8b91a159289ff74-1-1.23456-1518469636035");
        NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.HTTP, headers);

        // assertions
        DistributedTracePayloadImpl inboundPayload = transaction.getSpanProxy().getInboundDistributedTracePayload();
        assertEquals("da8bc8cc6d062849b0efcf3c169afb5a", inboundPayload.traceId);
        assertEquals("7d3efb1b173fecfa", inboundPayload.guid);
        assertEquals("33", inboundPayload.accountId);
        assertEquals("2827902", inboundPayload.applicationId);
        assertEquals("e8b91a159289ff74", inboundPayload.txnId);
        assertEquals(Sampled.SAMPLED_YES, inboundPayload.sampled);
        assertEquals(1.23456F, inboundPayload.priority, 0.0);
        assertEquals(1518469636035L, inboundPayload.timestamp);
    }

    @Test
    public void testInsert() {
        setupTransactionAndTracer();

        // make API call
        Headers headers = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        // assertions
        assertNotNull(headers.getHeader("traceparent"));
        assertNotNull(headers.getHeader("tracestate"));
        assertNotNull(headers.getHeader("newrelic"));
    }

    @Test
    public void testInsertExcludeNewRelicHeader() {
        // setup config
        setupConfig(true);
        setupTransactionAndTracer();

        // make API call
        Headers headers = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        // assertions
        assertNotNull(headers.getHeader("traceparent"));
        assertNotNull(headers.getHeader("tracestate"));
        assertNull(headers.getHeader("newrelic"));
    }

    @Test
    public void testAcceptThenInsert() {
        setupTransactionAndTracer();

        // call accept API
        Headers requestHeaders = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
        requestHeaders.setHeader("traceparent", "00-da8bc8cc6d062849b0efcf3c169afb5a-7d3efb1b173fecfa-01");
        requestHeaders.setHeader("tracestate", "33@nr=0-0-33-2827902-7d3efb1b173fecfa-e8b91a159289ff74-1-1.23456-1518469636035");
        NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.HTTP, requestHeaders);

        // call insert API
        Headers responseHeaders = ConcurrentHashMapHeaders.build(HeaderType.HTTP);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(responseHeaders);

        // assertions
        String traceparentPattern = "00-da8bc8cc6d062849b0efcf3c169afb5a-.{16}-01";
        String tracestatePattern = "33@nr=0-0-33-2827902-.{16}-.{16}-1-1.23456-.*";
        assertTrue(responseHeaders.getHeader("traceparent").matches(traceparentPattern));
        assertTrue(responseHeaders.getHeader("tracestate").matches(tracestatePattern));
        assertNotNull(responseHeaders.getHeader("newrelic"));
    }

}