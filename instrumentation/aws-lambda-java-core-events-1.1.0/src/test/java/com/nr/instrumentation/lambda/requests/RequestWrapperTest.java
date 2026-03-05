/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RequestWrapperTest {

    @Test
    public void testAPIGatewayProxyRequestEventNullHeadersAndParams() {
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setPath("/example");
        requestContext.setHttpMethod("GET");
        APIGatewayProxyRequestEvent apiGatewayEvent = new APIGatewayProxyRequestEvent();
        apiGatewayEvent.setRequestContext(requestContext);
        assertRequestWrapperNullHeadersAndParams(new APIGatewayProxyRequestWrapper(apiGatewayEvent));
    }

    @Test
    public void testAPIGatewayProxyRequestEvent() {
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent apiGatewayEvent = new APIGatewayProxyRequestEvent();
        apiGatewayEvent.setRequestContext(requestContext);
        apiGatewayEvent.setResource("/example");

        Map<String, String> headers = new HashMap<>();
        headers.put("K", "V");
        headers.put("Cookie", "PHPSESSID=298zf09hf012fh2; csrftoken=u32t4o3tb3gg43; _gat=1");
        headers.put("Hosts", "example.com, api.com");
        headers.put("Listings", "dot; com");
        apiGatewayEvent.setHeaders(headers);
        apiGatewayEvent.setHttpMethod("GET");
        apiGatewayEvent.setQueryStringParameters(Collections.singletonMap("Q", "Val"));
        apiGatewayEvent.setMultiValueQueryStringParameters(Collections.singletonMap("Line", Arrays.asList("1", "2", "3")));
        apiGatewayEvent.setMultiValueHeaders(Collections.singletonMap("HeaderKey", Arrays.asList("h1", "h2", "h3")));

        APIGatewayProxyRequestWrapper wrapper = new APIGatewayProxyRequestWrapper(apiGatewayEvent);
        assertRequestWrapper(wrapper);

        List<String> params = Collections.list(wrapper.getParameterNames());
        assertEquals(2, params.size());
        assertEquals("Line", params.get(0));
        assertEquals("Q", params.get(1));

        String[] multiValueParamValues = wrapper.getParameterValues("Line");
        assertEquals(3, multiValueParamValues.length);
        assertEquals("1", multiValueParamValues[0]);
        assertEquals("2", multiValueParamValues[1]);
        assertEquals("3", multiValueParamValues[2]);

        List<String> multiValueHeader = wrapper.getHeaders("HeaderKey");
        assertEquals(3, multiValueHeader.size());
        assertEquals("h1", multiValueHeader.get(0));
        assertEquals("h2", multiValueHeader.get(1));
        assertEquals("h3", multiValueHeader.get(2));
        assertEquals("h1", wrapper.getHeader("HeaderKey"));
    }

    @Test
    public void testAPIGatewayV2HTTPEventNullHeadersAndParams() {
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        requestContext.setHttp(APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build());
        APIGatewayV2HTTPEvent apiGatewayV2Event = new APIGatewayV2HTTPEvent();
        apiGatewayV2Event.setRequestContext(requestContext);
        apiGatewayV2Event.setRawPath("/example");
        assertRequestWrapperNullHeadersAndParams(new APIGatewayV2HttpRequestWrapper(apiGatewayV2Event));
    }

    @Test
    public void testAPIGatewayV2HTTPEvent() {
        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        requestContext.setHttp(APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build());
        APIGatewayV2HTTPEvent apiGatewayV2Event = new APIGatewayV2HTTPEvent();
        apiGatewayV2Event.setRequestContext(requestContext);
        apiGatewayV2Event.setRawPath("/example");
        Map<String, String> headers = new HashMap<>();
        headers.put("K", "V");
        apiGatewayV2Event.setCookies(Collections.singletonList("csrftoken=u32t4o3tb3gg43"));
        headers.put("Hosts", "example.com, api.com");
        headers.put("Listings", "dot; com");
        apiGatewayV2Event.setHeaders(headers);

        apiGatewayV2Event.setQueryStringParameters(Collections.singletonMap("Q", "Val"));

        APIGatewayV2HttpRequestWrapper wrapper = new APIGatewayV2HttpRequestWrapper(apiGatewayV2Event);
        assertRequestWrapper(wrapper);

        List<String> params = Collections.list(wrapper.getParameterNames());
        assertEquals(1, params.size());
        assertEquals("Q", params.get(0));
    }

    @Test
    public void testApplicationLoadBalancerRequestEventNullHeadersAndParams() {
        ApplicationLoadBalancerRequestEvent.Elb elb = new ApplicationLoadBalancerRequestEvent.Elb();

        ApplicationLoadBalancerRequestEvent.RequestContext requestContext = new ApplicationLoadBalancerRequestEvent.RequestContext();
        requestContext.setElb(elb);

        ApplicationLoadBalancerRequestEvent albEvent = new ApplicationLoadBalancerRequestEvent();
        albEvent.setPath("/example");
        albEvent.setHttpMethod("GET");
        albEvent.setRequestContext(requestContext);
        assertRequestWrapperNullHeadersAndParams(new ApplicationLoadBalancerRequestWrapper(albEvent));
    }

    @Test
    public void testApplicationLoadBalancerRequestEvent() {
        ApplicationLoadBalancerRequestEvent.Elb elb = new ApplicationLoadBalancerRequestEvent.Elb();

        ApplicationLoadBalancerRequestEvent.RequestContext requestContext = new ApplicationLoadBalancerRequestEvent.RequestContext();
        requestContext.setElb(elb);

        ApplicationLoadBalancerRequestEvent albEvent = new ApplicationLoadBalancerRequestEvent();
        albEvent.setPath("/example");
        albEvent.setHttpMethod("GET");
        albEvent.setRequestContext(requestContext);

        Map<String, String> headers = new HashMap<>();
        headers.put("K", "V");
        headers.put("Cookie", "PHPSESSID=298zf09hf012fh2; csrftoken=u32t4o3tb3gg43; _gat=1");
        headers.put("Hosts", "example.com, api.com");
        headers.put("Listings", "dot; com");
        albEvent.setHeaders(headers);

        albEvent.setQueryStringParameters(Collections.singletonMap("Q", "Val"));
        albEvent.setMultiValueQueryStringParameters(Collections.singletonMap("Line", Arrays.asList("1", "2", "3")));
        albEvent.setMultiValueHeaders(Collections.singletonMap("HeaderKey", Arrays.asList("h1", "h2", "h3")));

        ApplicationLoadBalancerRequestWrapper wrapper = new ApplicationLoadBalancerRequestWrapper(albEvent);
        assertRequestWrapper(wrapper);

        List<String> params = Collections.list(wrapper.getParameterNames());
        assertEquals(2, params.size());
        assertEquals("Line", params.get(0));
        assertEquals("Q", params.get(1));

        String[] multiValueParamValues = wrapper.getParameterValues("Line");
        assertEquals(3, multiValueParamValues.length);
        assertEquals("1", multiValueParamValues[0]);
        assertEquals("2", multiValueParamValues[1]);
        assertEquals("3", multiValueParamValues[2]);

        List<String> multiValueHeader = wrapper.getHeaders("HeaderKey");
        assertEquals(3, multiValueHeader.size());
        assertEquals("h1", multiValueHeader.get(0));
        assertEquals("h2", multiValueHeader.get(1));
        assertEquals("h3", multiValueHeader.get(2));
        assertEquals("h1", wrapper.getHeader("HeaderKey"));
    }



    private void assertRequestWrapperNullHeadersAndParams(ExtendedRequest request) {
        assertEquals("GET", request.getMethod());

        assertNull(request.getHeaders("K"));
        assertNull(request.getHeader("K"));

        assertEquals("/example", request.getRequestURI());

        assertNull(request.getCookieValue("csrftoken"));

        assertEquals(HeaderType.HTTP, request.getHeaderType());

        assertEquals(0, request.getParameterValues("Q").length);

        List<String> params = Collections.list(request.getParameterNames());
        assertEquals(0, params.size());
    }

    private void assertRequestWrapper(ExtendedRequest request) {
        assertEquals("GET", request.getMethod());
        assertEquals(1, request.getHeaders("K").size());
        assertEquals("V", request.getHeaders("K").get(0));

        assertEquals(2, request.getHeaders("Hosts").size());
        assertEquals("example.com", request.getHeaders("Hosts").get(0));
        assertEquals("api.com", request.getHeaders("Hosts").get(1));

        assertEquals(2, request.getHeaders("Hosts").size());
        assertEquals("dot", request.getHeaders("Listings").get(0));
        assertEquals("com", request.getHeaders("Listings").get(1));

        assertEquals("V", request.getHeader("K"));
        assertEquals("example.com", request.getHeader("Hosts"));
        assertEquals("dot", request.getHeader("Listings"));

        assertNull(request.getHeader("---"));
        assertNull(request.getHeaders("---"));

        assertEquals("/example", request.getRequestURI());

        assertEquals("u32t4o3tb3gg43", request.getCookieValue("csrftoken"));
        assertNull(request.getCookieValue("notExists"));

        assertEquals(HeaderType.HTTP, request.getHeaderType());

        assertEquals(1, request.getParameterValues("Q").length);
        assertEquals("Val", request.getParameterValues("Q")[0]);
    }
}
