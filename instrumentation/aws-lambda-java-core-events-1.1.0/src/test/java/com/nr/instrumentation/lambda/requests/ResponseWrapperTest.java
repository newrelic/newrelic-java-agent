/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import org.junit.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResponseWrapperTest {
    @Test
    public void testAPIGatewayProxyResponseEvent() throws Exception {
        APIGatewayProxyResponseEvent event = new APIGatewayProxyResponseEvent();
        event.setStatusCode(200);
        event.setBody("Hello World");
        assertResponseWrapper(new APIGatewayProxyResponseWrapper(event), event::getHeaders);
    }

    @Test
    public void testAPIGatewayV2HttpResponseEvent() throws Exception {
        APIGatewayV2HTTPResponse event = new APIGatewayV2HTTPResponse();
        event.setStatusCode(200);
        event.setBody("Hello World");
        assertResponseWrapper(new APIGatewayV2HttpResponseWrapper(event), event::getHeaders);
    }

    @Test
    public void testApplicationLoadBalancerResponseEvent() throws Exception {
        ApplicationLoadBalancerResponseEvent event = new ApplicationLoadBalancerResponseEvent();
        event.setStatusCode(200);
        event.setBody("Hello World");
        assertResponseWrapper(new ApplicationLoadBalancerResponseWrapper(event), event::getHeaders);
    }

    public void assertResponseWrapper(ExtendedResponse response, Supplier<Map<String, String>> headersSupplier) throws Exception {
        assertEquals(HeaderType.HTTP, response.getHeaderType());
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentLength() > 0);
        assertNull(response.getContentType());
        assertNull(null, headersSupplier.get());

        response.setHeader("Content-Type", "text/plain");
        response.setHeader("Content-Length", "10");
        assertEquals("text/plain", response.getContentType());
        assertEquals(10, response.getContentLength());


        response.setHeader("K", "V");
        assertEquals("text/plain", response.getContentType());
        assertEquals(10, response.getContentLength());
        assertEquals("V", headersSupplier.get().get("K"));

    }
}
