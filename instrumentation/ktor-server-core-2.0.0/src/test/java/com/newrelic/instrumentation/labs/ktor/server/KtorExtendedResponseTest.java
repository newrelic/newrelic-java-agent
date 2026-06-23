/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.server;

import com.newrelic.api.agent.HeaderType;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.response.ApplicationResponse;
import io.ktor.server.response.ResponseHeaders;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KtorExtendedResponseTest {

    private static class FakeResponseHeaders extends ResponseHeaders {
        private final Map<String, String> headerValues = new HashMap<>();
        private String lastAppendedName;
        private String lastAppendedValue;

        void put(String name, String value) {
            headerValues.put(name, value);
        }

        boolean wasAppended(String name, String value) {
            return name.equals(lastAppendedName) && value.equals(lastAppendedValue);
        }

        @Override
        public String get(String name) {
            return headerValues.get(name);
        }

        @Override
        protected void engineAppendHeader(String name, String value) {
            lastAppendedName = name;
            lastAppendedValue = value;
        }

        @Override
        protected List<String> getEngineHeaderNames() {
            return new ArrayList<>(headerValues.keySet());
        }

        @Override
        protected List<String> getEngineHeaderValues(String name) {
            List<String> result = new ArrayList<>();
            String v = headerValues.get(name);
            if (v != null) result.add(v);
            return result;
        }
    }

    private ApplicationCall call;
    private ApplicationResponse response;
    private FakeResponseHeaders headers;
    private KtorExtendedResponse extendedResponse;

    @Before
    public void setup() {
        call = mock(ApplicationCall.class);
        response = mock(ApplicationResponse.class);
        headers = new FakeResponseHeaders();
        when(call.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        extendedResponse = new KtorExtendedResponse(call);
    }

    @Test
    public void getHeaderType_returnsHTTP() {
        assertEquals(HeaderType.HTTP, extendedResponse.getHeaderType());
    }

    @Test
    public void getStatus_returnsStatusValue() throws Exception {
        when(response.status()).thenReturn(new HttpStatusCode(200, "OK"));

        assertEquals(200, extendedResponse.getStatus());
    }

    @Test
    public void getStatus_returnsZeroWhenStatusNull() throws Exception {
        when(response.status()).thenReturn(null);

        assertEquals(0, extendedResponse.getStatus());
    }

    @Test
    public void getStatus_returnsZeroWhenResponseNull() throws Exception {
        when(call.getResponse()).thenReturn(null);

        assertEquals(0, extendedResponse.getStatus());
    }

    @Test
    public void getStatusMessage_returnsDescription() throws Exception {
        when(response.status()).thenReturn(new HttpStatusCode(200, "OK"));

        assertEquals("OK", extendedResponse.getStatusMessage());
    }

    @Test
    public void getContentType_readsFromResponseHeaders() {
        headers.put("Content-Type", "application/json");

        assertEquals("application/json", extendedResponse.getContentType());
    }

    @Test
    public void getContentType_returnsNullWhenHeaderAbsent() {
        assertNull(extendedResponse.getContentType());
    }

    @Test
    public void getContentType_returnsNullWhenResponseNull() {
        when(call.getResponse()).thenReturn(null);

        assertNull(extendedResponse.getContentType());
    }

    @Test
    public void getContentLength_readsFromResponseHeaders() {
        headers.put("Content-Length", "1024");

        assertEquals(1024L, extendedResponse.getContentLength());
    }

    @Test
    public void getContentLength_returnsZeroForNonNumericValue() {
        headers.put("Content-Length", "bad");

        assertEquals(0L, extendedResponse.getContentLength());
    }

    @Test
    public void getContentLength_returnsZeroWhenResponseNull() {
        when(call.getResponse()).thenReturn(null);

        assertEquals(0L, extendedResponse.getContentLength());
    }

    @Test
    public void setHeader_appendsToResponseHeaders() {
        extendedResponse.setHeader("X-H", "v");

        assertTrue(headers.wasAppended("X-H", "v"));
    }
}
