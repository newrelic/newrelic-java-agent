/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.netty;

import io.ktor.http.HttpMethod;
import io.ktor.http.RequestConnectionPoint;
import kotlin.coroutines.CoroutineContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilsTest {

    @Test
    public void getTransactionName_withPathAndMethod() {
        RequestConnectionPoint point = mock(RequestConnectionPoint.class);
        when(point.getUri()).thenReturn("/api/test");
        when(point.getMethod()).thenReturn(HttpMethod.Companion.getGet());

        assertEquals("api/test - {GET}", Utils.getTransactionName(point));
    }

    @Test
    public void getTransactionName_withRootUri_returnsRoot() {
        RequestConnectionPoint point = mock(RequestConnectionPoint.class);
        when(point.getUri()).thenReturn("/");
        when(point.getMethod()).thenReturn(null);

        assertEquals("Root", Utils.getTransactionName(point));
    }

    @Test
    public void getTransactionName_withNullUri_returnsJustMethod() {
        RequestConnectionPoint point = mock(RequestConnectionPoint.class);
        when(point.getUri()).thenReturn(null);
        when(point.getMethod()).thenReturn(HttpMethod.Companion.getPost());

        assertEquals(" - {POST}", Utils.getTransactionName(point));
    }

    @Test
    public void getTransactionName_withNullPointMethod() {
        RequestConnectionPoint point = mock(RequestConnectionPoint.class);
        when(point.getUri()).thenReturn("/page");
        when(point.getMethod()).thenReturn(null);

        assertEquals("page", Utils.getTransactionName(point));
    }

    @Test
    public void getApplicationName_returnsNullForNullApp() {
        assertNull(Utils.getApplicationName(null));
    }

    @Test
    public void getCoroutineName_returnsNullForEmptyContext() {
        CoroutineContext context = mock(CoroutineContext.class);
        when(context.get(any())).thenReturn(null);

        assertNull(Utils.getCoroutineName(context));
    }
}
