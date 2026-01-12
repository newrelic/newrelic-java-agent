/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.agent.util.MockDistributedTraceService;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category(RequiresFork.class)
public class HeadersUtilTest {
    @Test
    public void createDTHeadersSetsSpanIdEvenIfTxNotSampled() {
        ConfigService mockConfigService = new MockConfigService(AgentConfigImpl.createAgentConfig(
                ImmutableMap.of(
                        AgentConfigImpl.APP_NAME,
                        "Unit Test",
                        AgentConfigImpl.DISTRIBUTED_TRACING,
                        Collections.singletonMap(DistributedTracingConfig.ENABLED, true),
                        AgentConfigImpl.SPAN_EVENTS,
                        ImmutableMap.of(
                                SpanEventsConfig.ENABLED, true,
                                SpanEventsConfig.COLLECT_SPAN_EVENTS, true)
                )));
        MockServiceManager mockServiceManager = new MockServiceManager(mockConfigService);
        mockServiceManager.setDistributedTraceService(new MockDistributedTraceService());
        ServiceFactory.setServiceManager(mockServiceManager);
        Transaction tx = Transaction.getTransaction();
        tx.setPriorityIfNotNull(0F);
        tx.startTransactionIfBeginning(new MockDispatcherTracer(tx));

        Tracer mockTracer = new DefaultTracer(tx, new ClassMethodSignature(getClass().getName(), "tracerMethod", "()V"), this);

        OutboundHeadersMap map = new OutboundHeadersMap(HeaderType.HTTP);

        assertTrue("DT headers should have been set", HeadersUtil.createAndSetDistributedTraceHeaders(tx, mockTracer, map));
        assertFalse(tx.sampled());

        assertTrue("map should contain newrelic header", map.containsKey("newrelic"));
        String decodedHeader = new String(Base64.getDecoder().decode(map.get("newrelic")), StandardCharsets.UTF_8);
        JsonObject jsonObject = new Gson().fromJson(decodedHeader, JsonObject.class);
        assertFalse("d.sa should be false because tx is not sampled", jsonObject.getAsJsonObject("d").get("sa").getAsBoolean());
        assertEquals("d.pr should be zero", 0f, jsonObject.getAsJsonObject("d").get("pr").getAsFloat(), 0.0001);
        assertNotNull("d.id should not be null", jsonObject.getAsJsonObject("d").get("id"));
        assertEquals("d.id should be the span id.", mockTracer.getGuid(), jsonObject.getAsJsonObject("d").get("id").getAsString());

        String traceParent = map.get("traceparent");
        assertEquals("traceparent parentId field should match span id.", mockTracer.getGuid(), traceParent.split("-")[2]);

        String traceState = map.get("tracestate");
        assertEquals("tracestate spanId field should match span id.", mockTracer.getGuid(), traceState.split("-")[4]);
        assertEquals("tracestate txId field should match tx id.", tx.getGuid(), traceState.split("-")[5]);
    }

    @Test
    public void getTraceHeader() {
        assertEquals("abc",
                HeadersUtil.getNewRelicTraceHeader(createInboundHeaders(ImmutableMap.of("newrelic", "abc"), HeaderType.HTTP)));

        assertEquals("def",
                HeadersUtil.getNewRelicTraceHeader(createInboundHeaders(ImmutableMap.of("NEWRELIC", "def"), HeaderType.HTTP)));

        assertEquals("ghi",
                HeadersUtil.getNewRelicTraceHeader(createInboundHeaders(ImmutableMap.of("Newrelic", "ghi"), HeaderType.HTTP)));
    }

    @Test
    public void testGetSyntheticsInfoHeader() {
        String synthInfoValue = "{\n" +
                "       \"version\": \"1\",\n" +
                "       \"type\": \"scheduled\",\n" +
                "       \"initiator\": \"cli\",\n" +
                "       \"attributes\": {\n" +
                "           \"example1\": \"Value1\",\n" +
                "           \"example2\": \"Value2\"\n" +
                "           }\n" +
                "}";

        assertEquals(synthInfoValue, HeadersUtil.getSyntheticsInfoHeader(createInboundHeaders
                (ImmutableMap.of("X-NewRelic-Synthetics-Info", synthInfoValue), HeaderType.HTTP)));
    }

    @Test
    public void testInboundParentSampledTrueConfigAlwaysOn() {
        Transaction tx = setupAndCreateTx(SamplerConfig.ALWAYS_ON, SamplerConfig.DEFAULT);
        InboundHeaders inboundHeaders = createInboundHeaders(ImmutableMap.of(
                "traceparent", "01-0123456789abcdef0123456789abcdef-0123456789abcdef-01", // last entry is the sampled flag = true
                "tracestate", "trustyrusty@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05--0.5-1563574856827"
        ), HeaderType.HTTP);

        HeadersUtil.parseAndAcceptDistributedTraceHeaders(tx, inboundHeaders);
        assertTrue(tx.getSpanProxy().getInitiatingW3CTraceParent().sampled());
        assertTrue(tx.getPriority() == 3.0f);

        Transaction.clearTransaction();
    }

    @Test
    public void testInboundParentSampledTrueConfigAlwaysOff() {
        Transaction tx = setupAndCreateTx(SamplerConfig.ALWAYS_OFF, SamplerConfig.DEFAULT);
        InboundHeaders inboundHeaders = createInboundHeaders(ImmutableMap.of(
                "traceparent", "01-0123456789abcdef0123456789abcdef-0123456789abcdef-01", // last entry is the sampled flag = true
                "tracestate", "trustyrusty@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05--0.5-1563574856827"
        ), HeaderType.HTTP);

        HeadersUtil.parseAndAcceptDistributedTraceHeaders(tx, inboundHeaders);
        assertTrue(tx.getSpanProxy().getInitiatingW3CTraceParent().sampled());
        assertTrue(tx.getPriority() == 0.0f);

        Transaction.clearTransaction();
    }

    @Test
    public void testInboundParentSampledFalseConfigAlwaysOn() {
        Transaction tx = setupAndCreateTx(SamplerConfig.DEFAULT, SamplerConfig.ALWAYS_ON);
        InboundHeaders inboundHeaders = createInboundHeaders(ImmutableMap.of(
                "traceparent", "01-0123456789abcdef0123456789abcdef-0123456789abcdef-00", // last entry is the sampled flag = true
                "tracestate", "trustyrusty@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05--0.5-1563574856827"
        ), HeaderType.HTTP);

        HeadersUtil.parseAndAcceptDistributedTraceHeaders(tx, inboundHeaders);
        assertFalse(tx.getSpanProxy().getInitiatingW3CTraceParent().sampled());
        assertTrue(tx.getPriority() == 3.0f);

        Transaction.clearTransaction();
    }

    @Test
    public void testInboundParentSampledFalseConfigAlwaysOff() {
        Transaction tx = setupAndCreateTx(SamplerConfig.DEFAULT, SamplerConfig.ALWAYS_OFF);
        InboundHeaders inboundHeaders = createInboundHeaders(ImmutableMap.of(
                "traceparent", "01-0123456789abcdef0123456789abcdef-0123456789abcdef-00", // last entry is the sampled flag = true
                "tracestate", "trustyrusty@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05--0.5-1563574856827"
        ), HeaderType.HTTP);

        HeadersUtil.parseAndAcceptDistributedTraceHeaders(tx, inboundHeaders);
        assertFalse(tx.getSpanProxy().getInitiatingW3CTraceParent().sampled());
        assertTrue(tx.getPriority() == 0.0f);

        Transaction.clearTransaction();
    }

    private Transaction setupAndCreateTx(String remoteParentSampled, String remoteParentNotSampled) {
        System.out.println("Setting up config: " + remoteParentSampled + "; " + remoteParentNotSampled);
        ConfigService mockConfigService = new MockConfigService(AgentConfigImpl.createAgentConfig(
                ImmutableMap.of(
                        AgentConfigImpl.APP_NAME,
                        "Unit Test",
                        AgentConfigImpl.DISTRIBUTED_TRACING,
                        ImmutableMap.of(
                                DistributedTracingConfig.ENABLED, true,
                                SamplerConfig.SAMPLER_CONFIG_ROOT,
                                ImmutableMap.of(
                                        SamplerConfig.REMOTE_PARENT_SAMPLED, remoteParentSampled,
                                        SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, remoteParentNotSampled)
                        ),
                        AgentConfigImpl.SPAN_EVENTS,
                        ImmutableMap.of(
                                SpanEventsConfig.ENABLED, true,
                                SpanEventsConfig.COLLECT_SPAN_EVENTS, true)
                )));
        MockServiceManager mockServiceManager = new MockServiceManager(mockConfigService);
        mockServiceManager.setDistributedTraceService(new MockDistributedTraceService());
        ServiceFactory.setServiceManager(mockServiceManager);
        Transaction tx = Mockito.spy(Transaction.getTransaction());
        TransactionTimer timer = Mockito.mock(TransactionTimer.class);
        Mockito.when(timer.getStartTimeInNanos()).thenReturn(System.nanoTime());
        Mockito.when(tx.getTransactionTimer()).thenReturn(timer);
        tx.setPriorityIfNotNull(0.5f);  // something > 0 and < 1

        return tx;
    }

    private InboundHeaders createInboundHeaders(final Map<String, String> map, final HeaderType type) {
        return new InboundHeaders() {
            @Override
            public HeaderType getHeaderType() {
                return type;
            }

            @Override
            public String getHeader(String name) {
                return map.get(name);
            }
        };
    }
}