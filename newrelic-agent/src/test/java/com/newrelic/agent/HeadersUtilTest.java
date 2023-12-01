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
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.MockDistributedTraceService;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

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