/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.MockDistributedTraceService;
import com.newrelic.api.agent.DistributedTracePayload;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpanProxyTest {
    @Before
    public void before() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "Test");

        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());

        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        serviceManager.setConfigService(configService);
        ServiceFactory.getServiceManager().start();

        serviceManager.setDistributedTraceService(new MockDistributedTraceService());
    }

    @Test
    public void testTraceId_noneYetSet() throws Exception {
        SpanProxy spanProxy = new SpanProxy();
        String traceId = spanProxy.getOrCreateTraceId();
        assertNotNull(traceId);
    }
    
    @Test
    public void testTraceIdFromInboundPayload() throws Exception {
        SpanProxy spanProxy = new SpanProxy();
        String traceId = "flimflam";
        DistributedTracePayload payload = new DistributedTracePayloadImpl(0, null, null, null, null, null, traceId, null, null, null);
        spanProxy.acceptDistributedTracePayload(payload);
        String result = spanProxy.getOrCreateTraceId();
        assertEquals(traceId, result);
    }
    
    @Test
    public void testTraceIdFromOutboundPayload() throws Exception {
        SpanProxy spanProxy = new SpanProxy();
        String traceId = spanProxy.getOrCreateTraceId();
        DistributedTracePayloadImpl dtPayload = (DistributedTracePayloadImpl) spanProxy.createDistributedTracePayload(1.34f, null, null);
        String result = spanProxy.getOrCreateTraceId();
        assertEquals(result, traceId);
        assertEquals(traceId, dtPayload.traceId);
    }
    
    @Test
    public void testTraceIdFromW3cPayload() throws Exception {
        SpanProxy spanProxy = new SpanProxy();
        String traceId = "flimflam";
        W3CTraceParent traceParent = new W3CTraceParent(null, traceId, null, 0);
        spanProxy.setInitiatingW3CTraceParent(traceParent);
        String result = spanProxy.getOrCreateTraceId();
        assertEquals(traceId, result);
    }

    @Test
    public void testGetCreateGet() {
        SpanProxy spanProxy = new SpanProxy();
        assertNull(spanProxy.getTraceId());
        String result = spanProxy.getOrCreateTraceId();
        assertNotNull(result);
        assertEquals(result, spanProxy.getTraceId());
    }

    @Test
    public void testGetCreateAcceptGet() {
        String dtTraceId = "flimflam";
        SpanProxy spanProxy = new SpanProxy();
        assertNull(spanProxy.getTraceId());
        String result = spanProxy.getOrCreateTraceId();
        // even though we accept the payload, we don't change the traceId if we already set it.
        DistributedTracePayloadImpl dtTracePayload = new DistributedTracePayloadImpl(0, null, null, null, null, null, dtTraceId, null, null, null);

        boolean accepted = spanProxy.acceptDistributedTracePayload(dtTracePayload);

        assertTrue(accepted);
        assertNotNull(result);
        assertEquals(dtTraceId, spanProxy.getTraceId());
        assertNotEquals(result, spanProxy.getTraceId());
    }
}