/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.server;

import com.newrelic.api.agent.HeaderType;
import io.ktor.http.Headers;
import io.ktor.http.Parameters;
import io.ktor.http.RequestConnectionPoint;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.request.ApplicationRequest;
import io.ktor.server.request.RequestCookies;
import io.ktor.util.Attributes;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KtorExtendedRequestTest {

    private ApplicationCall call;
    private ApplicationRequest request;
    private KtorExtendedRequest extendedRequest;

    @Before
    public void setup() {
        call = mock(ApplicationCall.class);
        request = mock(ApplicationRequest.class);
        when(call.getRequest()).thenReturn(request);
        extendedRequest = new KtorExtendedRequest(call);
    }

    @Test
    public void getHeaderType_returnsHTTP() {
        assertEquals(HeaderType.HTTP, extendedRequest.getHeaderType());
    }

    @Test
    public void getRequestURI_returnsUriFromLocal() {
        RequestConnectionPoint local = mock(RequestConnectionPoint.class);
        when(request.getLocal()).thenReturn(local);
        when(local.getUri()).thenReturn("/api/users");

        assertEquals("/api/users", extendedRequest.getRequestURI());
    }

    @Test
    public void getRequestURI_returnsNullWhenRequestNull() {
        when(call.getRequest()).thenReturn(null);

        assertNull(extendedRequest.getRequestURI());
    }

    @Test
    public void getHeader_returnsHeaderValue() {
        Headers headers = mock(Headers.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Test")).thenReturn("value");

        assertEquals("value", extendedRequest.getHeader("X-Test"));
    }

    @Test
    public void getHeader_returnsNullWhenRequestNull() {
        when(call.getRequest()).thenReturn(null);

        assertNull(extendedRequest.getHeader("X-Test"));
    }

    @Test
    public void getAttribute_returnsNullForMissingAttribute() {
        Attributes attrs = mock(Attributes.class);
        when(call.getAttributes()).thenReturn(attrs);
        when(attrs.getOrNull(any())).thenReturn(null);

        assertNull(extendedRequest.getAttribute("missingKey"));
    }

    @Test
    public void getAttribute_returnsValueWhenPresent() {
        Attributes attrs = mock(Attributes.class);
        when(call.getAttributes()).thenReturn(attrs);
        when(attrs.getOrNull(any())).thenReturn("val");

        assertEquals("val", extendedRequest.getAttribute("someKey"));
    }

    @Test
    public void getAttribute_returnsNullWhenAttributesNull() {
        when(call.getAttributes()).thenReturn(null);

        assertNull(extendedRequest.getAttribute("anyKey"));
    }

    @Test
    public void getParameterValues_returnsValuesForExistingParam() {
        Parameters params = mock(Parameters.class);
        when(request.getQueryParameters()).thenReturn(params);
        when(params.getAll("q")).thenReturn(Arrays.asList("a", "b"));

        String[] result = extendedRequest.getParameterValues("q");
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    public void getParameterValues_returnsEmptyArrayWhenRequestNull() {
        when(call.getRequest()).thenReturn(null);

        assertArrayEquals(new String[0], extendedRequest.getParameterValues("q"));
    }

    @Test
    public void getParameterNames_hasElementsWhenParamsExist() {
        Parameters params = mock(Parameters.class);
        when(request.getQueryParameters()).thenReturn(params);
        Set<String> names = new HashSet<>(Arrays.asList("page", "size"));
        when(params.names()).thenReturn(names);

        java.util.Enumeration<?> enumeration = extendedRequest.getParameterNames();
        assertNotNull(enumeration);
        Set<String> result = new HashSet<>();
        while (enumeration.hasMoreElements()) {
            result.add((String) enumeration.nextElement());
        }
        assertEquals(names, result);
    }

    @Test
    public void getParameterNames_returnsEmptyEnumerationWhenRequestNull() {
        when(call.getRequest()).thenReturn(null);

        java.util.Enumeration<?> enumeration = extendedRequest.getParameterNames();
        assertNotNull(enumeration);
        assertEquals(false, enumeration.hasMoreElements());
    }

    @Test
    public void getCookieValue_returnsCookieValue() {
        final Map<String, String> rawCookies = new HashMap<>();
        rawCookies.put("session", "abc");
        RequestCookies cookies = new RequestCookies(request) {
            @Override
            protected Map<String, String> fetchCookies() {
                return rawCookies;
            }
        };
        when(request.getCookies()).thenReturn(cookies);

        assertEquals("abc", extendedRequest.getCookieValue("session"));
    }

    @Test
    public void getRemoteUser_returnsNull() {
        assertNull(extendedRequest.getRemoteUser());
    }
}
