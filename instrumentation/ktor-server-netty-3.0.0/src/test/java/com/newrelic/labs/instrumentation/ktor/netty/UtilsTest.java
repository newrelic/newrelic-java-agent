package com.newrelic.labs.instrumentation.ktor.netty;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
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
        HttpRequest request = mock(HttpRequest.class);
        when(request.getUri()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        assertEquals("api/users - {GET}", Utils.getTransactionName(request));
    }

    @Test
    public void getTransactionName_withRootUri() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getUri()).thenReturn("/");
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        assertEquals("Root - {GET}", Utils.getTransactionName(request));
    }

    @Test
    public void getTransactionName_withNullUri() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getUri()).thenReturn(null);
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        assertEquals(" - {GET}", Utils.getTransactionName(request));
    }

    @Test
    public void getTransactionName_withNullMethod() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getUri()).thenReturn("/page");
        when(request.getMethod()).thenReturn(null);

        assertEquals("page", Utils.getTransactionName(request));
    }

    @Test
    public void getCoroutineName_returnsNullForEmptyContext() {
        CoroutineContext context = mock(CoroutineContext.class);
        when(context.get(any())).thenReturn(null);

        assertNull(Utils.getCoroutineName(context));
    }
}
