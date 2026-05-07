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

import java.util.List;
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
        APIGatewayProxyResponseWrapper wrapper = new APIGatewayProxyResponseWrapper(event);
        assertResponseWrapper(wrapper, event::getHeaders, event::getMultiValueHeaders);
        assertNull(wrapper.getStatusMessage());
    }

    @Test
    public void testAPIGatewayV2HttpResponseEvent() throws Exception {
        APIGatewayV2HTTPResponse event = new APIGatewayV2HTTPResponse();
        event.setStatusCode(200);
        event.setBody("Hello World");
        APIGatewayV2HttpResponseWrapper wrapper = new APIGatewayV2HttpResponseWrapper(event);
        assertResponseWrapper(wrapper, event::getHeaders, event::getMultiValueHeaders);
        assertNull(wrapper.getStatusMessage());
    }

    @Test
    public void testApplicationLoadBalancerResponseEvent() throws Exception {
        ApplicationLoadBalancerResponseEvent event = new ApplicationLoadBalancerResponseEvent();
        event.setStatusCode(200);
        event.setBody("Hello World");
        event.setStatusDescription("My Description");
        ApplicationLoadBalancerResponseWrapper wrapper = new  ApplicationLoadBalancerResponseWrapper(event);
        assertResponseWrapper(wrapper, event::getHeaders, event::getMultiValueHeaders);
        assertEquals("My Description", wrapper.getStatusMessage());
    }

    public void assertResponseWrapper(ExtendedResponse response, Supplier<Map<String, String>> headersSupplier,
            Supplier<Map<String, List<String>>> multiValueHeadersSupplier) throws Exception {
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
        assertEquals(0, multiValueHeadersSupplier.get().size());

        response.setHeader("K", "V2");
        assertEquals("V, V2", headersSupplier.get().get("K"));
        assertEquals(2, multiValueHeadersSupplier.get().get("K").size());
        assertEquals("V", multiValueHeadersSupplier.get().get("K").get(0));
        assertEquals("V2", multiValueHeadersSupplier.get().get("K").get(1));

        response.setHeader("K", "V3");
        assertEquals("V, V2, V3", headersSupplier.get().get("K"));
        assertEquals(3, multiValueHeadersSupplier.get().get("K").size());
        assertEquals("V", multiValueHeadersSupplier.get().get("K").get(0));
        assertEquals("V2", multiValueHeadersSupplier.get().get("K").get(1));
        assertEquals("V3", multiValueHeadersSupplier.get().get("K").get(2));
    }
}
