/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.client;

import com.newrelic.api.agent.HeaderType;
import io.ktor.client.request.HttpRequestBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KtorRequestHeadersTest {

    private KtorRequestHeaders headers;

    @Before
    public void setup() {
        headers = new KtorRequestHeaders(new HttpRequestBuilder());
    }

    @Test
    public void getHeaderType_returnsHTTP() {
        assertEquals(HeaderType.HTTP, headers.getHeaderType());
    }

    @Test
    public void setHeader_and_getHeader_roundtrip() {
        headers.setHeader("X-Test", "value1");

        assertEquals("value1", headers.getHeader("X-Test"));
    }

    @Test
    public void getHeader_returnsNullForMissingHeader() {
        assertNull(headers.getHeader("X-Missing"));
    }

    @Test
    public void addHeader_appendsMultipleValues() {
        headers.addHeader("X-Multi", "a");
        headers.addHeader("X-Multi", "b");

        Collection<String> values = headers.getHeaders("X-Multi");
        assertTrue(values.contains("a"));
        assertTrue(values.contains("b"));
    }

    @Test
    public void setHeader_overwritesPreviousValue() {
        headers.setHeader("X-Overwrite", "first");
        headers.setHeader("X-Overwrite", "second");

        assertEquals("second", headers.getHeader("X-Overwrite"));
    }

    @Test
    public void getHeaderNames_includesSetHeaders() {
        headers.setHeader("X-Name1", "v1");
        headers.setHeader("X-Name2", "v2");

        Collection<String> names = headers.getHeaderNames();
        assertTrue(names.contains("X-Name1"));
        assertTrue(names.contains("X-Name2"));
    }

    @Test
    public void containsHeader_trueAfterSet() {
        headers.setHeader("X-Present", "yes");

        assertTrue(headers.containsHeader("X-Present"));
    }

    @Test
    public void containsHeader_falseForAbsent() {
        assertFalse(headers.containsHeader("X-Absent"));
    }
}
