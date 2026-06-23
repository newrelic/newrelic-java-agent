/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.netty;

import com.newrelic.api.agent.HeaderType;
import io.ktor.http.HeadersKt;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KtorNettyHeadersTest {

    private KtorNettyHeaders headers;
    private KtorNettyHeaders emptyHeaders;

    @Before
    public void setup() {
        headers = new KtorNettyHeaders(HeadersKt.headersOf("X-Test", "val"));
        emptyHeaders = new KtorNettyHeaders(HeadersKt.headersOf());
    }

    @Test
    public void getHeaderType_returnsHTTP() {
        assertEquals(HeaderType.HTTP, headers.getHeaderType());
    }

    @Test
    public void getHeader_returnsValueForExistingHeader() {
        assertEquals("val", headers.getHeader("X-Test"));
    }

    @Test
    public void getHeader_returnsNullForMissingHeader() {
        assertNull(emptyHeaders.getHeader("X-Missing"));
    }

    @Test
    public void getHeaders_returnsAllValuesForHeader() {
        Collection<String> values = headers.getHeaders("X-Test");
        assertFalse(values.isEmpty());
        assertTrue(values.contains("val"));
    }

    @Test
    public void containsHeader_trueForPresentHeader() {
        assertTrue(headers.containsHeader("X-Test"));
    }

    @Test
    public void containsHeader_falseForAbsentHeader() {
        assertFalse(emptyHeaders.containsHeader("X-Missing"));
    }

    @Test
    public void getHeaderNames_includesHeaderName() {
        Collection<String> names = headers.getHeaderNames();
        assertTrue(names.contains("X-Test"));
    }
}
