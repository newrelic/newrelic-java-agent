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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
        requestContext.setPath("/example");
        APIGatewayProxyRequestEvent apiGatewayEvent = new APIGatewayProxyRequestEvent();
        apiGatewayEvent.setRequestContext(requestContext);

        Map<String, String> headers = new HashMap<>();
        headers.put("K", "V");
        headers.put("Cookie", "PHPSESSID=298zf09hf012fh2; csrftoken=u32t4o3tb3gg43; _gat=1");
        apiGatewayEvent.setHeaders(headers);
        apiGatewayEvent.setHttpMethod("GET");
        apiGatewayEvent.setQueryStringParameters(Collections.singletonMap("Q", "Val"));
        assertRequestWrapper(new APIGatewayProxyRequestWrapper(apiGatewayEvent));
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
        apiGatewayV2Event.setHeaders(headers);

        apiGatewayV2Event.setQueryStringParameters(Collections.singletonMap("Q", "Val"));
        assertRequestWrapper(new APIGatewayV2HttpRequestWrapper(apiGatewayV2Event));
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
        albEvent.setHeaders(headers);

        albEvent.setQueryStringParameters(Collections.singletonMap("Q", "Val"));

        assertRequestWrapper(new ApplicationLoadBalancerRequestWrapper(albEvent));
    }



    private void assertRequestWrapperNullHeadersAndParams(ExtendedRequest request) {
        assertEquals("GET", request.getMethod());
        assertEquals(0, request.getHeaders("K").size());
        assertEquals(null, request.getHeader("K"));
        assertEquals("/example", request.getRequestURI());
        assertEquals(null, request.getCookieValue("csrftoken"));
        assertEquals(HeaderType.HTTP, request.getHeaderType());
        assertEquals(0, request.getParameterValues("Q").length);
    }

    private void assertRequestWrapper(ExtendedRequest request) {
        assertEquals("GET", request.getMethod());
        assertEquals(1, request.getHeaders("K").size());
        assertEquals("V", request.getHeaders("K").get(0));
        assertEquals("V", request.getHeader("K"));
        assertEquals("/example", request.getRequestURI());
        assertEquals("u32t4o3tb3gg43", request.getCookieValue("csrftoken"));
        assertEquals(HeaderType.HTTP, request.getHeaderType());
        assertEquals(1, request.getParameterValues("Q").length);
        assertEquals("Val", request.getParameterValues("Q")[0]);
    }
}
