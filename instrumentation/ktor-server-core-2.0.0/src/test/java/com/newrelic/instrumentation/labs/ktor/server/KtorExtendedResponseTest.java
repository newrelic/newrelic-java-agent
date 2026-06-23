package com.newrelic.instrumentation.labs.ktor.server;

import com.newrelic.api.agent.HeaderType;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.response.ApplicationResponse;
import io.ktor.server.response.ResponseHeaders;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KtorExtendedResponseTest {

    private ApplicationCall call;
    private ApplicationResponse response;
    private ResponseHeaders headers;
    private KtorExtendedResponse extendedResponse;

    @Before
    public void setup() {
        call = mock(ApplicationCall.class);
        response = mock(ApplicationResponse.class);
        headers = mock(ResponseHeaders.class);
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
        HttpStatusCode statusCode = mock(HttpStatusCode.class);
        when(response.status()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn(200);

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
        HttpStatusCode statusCode = mock(HttpStatusCode.class);
        when(response.status()).thenReturn(statusCode);
        when(statusCode.getDescription()).thenReturn("OK");

        assertEquals("OK", extendedResponse.getStatusMessage());
    }

    @Test
    public void getContentType_readsFromResponseHeaders() {
        when(headers.get("Content-Type")).thenReturn("application/json");

        assertEquals("application/json", extendedResponse.getContentType());
    }

    @Test
    public void getContentType_returnsNullWhenHeaderAbsent() {
        when(headers.get("Content-Type")).thenReturn(null);

        assertNull(extendedResponse.getContentType());
    }

    @Test
    public void getContentType_returnsNullWhenResponseNull() {
        when(call.getResponse()).thenReturn(null);

        assertNull(extendedResponse.getContentType());
    }

    @Test
    public void getContentLength_readsFromResponseHeaders() {
        when(headers.get("Content-Length")).thenReturn("1024");

        assertEquals(1024L, extendedResponse.getContentLength());
    }

    @Test
    public void getContentLength_returnsZeroForNonNumericValue() {
        when(headers.get("Content-Length")).thenReturn("bad");

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

        verify(headers).append("X-H", "v", true);
    }
}
