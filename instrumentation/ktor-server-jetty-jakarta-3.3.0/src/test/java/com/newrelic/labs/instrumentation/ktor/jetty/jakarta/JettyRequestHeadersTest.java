/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.jetty.jakarta;

import com.newrelic.api.agent.HeaderType;
import org.eclipse.jetty.http.HttpFields;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JettyRequestHeadersTest {

    private JettyRequestHeaders headers;
    private JettyRequestHeaders emptyHeaders;
    private JettyRequestHeaders nullHeaders;

    @Before
    public void setup() {
        HttpFields.Mutable fields = HttpFields.build();
        fields.add("X-Test", "testValue");
        headers = new JettyRequestHeaders(fields);

        emptyHeaders = new JettyRequestHeaders(HttpFields.build());
        nullHeaders = new JettyRequestHeaders(null);
    }

    @Test
    public void getHeaderType_returnsHTTP() {
        assertEquals(HeaderType.HTTP, headers.getHeaderType());
    }

    @Test
    public void getHeader_returnsHeaderValue() {
        assertEquals("testValue", headers.getHeader("X-Test"));
    }

    @Test
    public void getHeader_returnsNullForMissing() {
        assertNull(emptyHeaders.getHeader("X-Missing"));
    }

    @Test
    public void getHeaders_returnsAllValues() {
        Collection<String> values = headers.getHeaders("X-Test");
        assertFalse(values.isEmpty());
        assertTrue(values.contains("testValue"));
    }

    @Test
    public void getHeaders_returnsEmptyForMissingHeader() {
        Collection<String> values = emptyHeaders.getHeaders("X-None");
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void getHeaders_returnsEmptyForNull() {
        Collection<String> values = nullHeaders.getHeaders("X-Any");
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void containsHeader_trueWhenHeaderPresent() {
        assertTrue(headers.containsHeader("X-Test"));
    }

    @Test
    public void containsHeader_falseWhenHeaderAbsent() {
        assertFalse(emptyHeaders.containsHeader("X-Missing"));
    }

    @Test
    public void getHeaderNames_includesHeaderName() {
        Collection<String> names = headers.getHeaderNames();
        assertTrue(names.contains("X-Test"));
    }
}
