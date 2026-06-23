package com.newrelic.labs.instrumentation.ktor.servlet;

import com.newrelic.api.agent.HeaderType;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServletRequestHeadersTest {

    private HttpServletRequest request;
    private ServletRequestHeaders headers;

    @Before
    public void setup() {
        request = mock(HttpServletRequest.class);
        headers = new ServletRequestHeaders(request);
    }

    @Test
    public void getHeaderType_returnsHTTP() {
        assertEquals(HeaderType.HTTP, headers.getHeaderType());
    }

    @Test
    public void getHeader_returnsHeaderValue() {
        when(request.getHeader("X-Test")).thenReturn("v");

        assertEquals("v", headers.getHeader("X-Test"));
    }

    @Test
    public void getHeader_returnsNullForMissing() {
        when(request.getHeader("X-Missing")).thenReturn(null);

        assertEquals(null, headers.getHeader("X-Missing"));
    }

    @Test
    public void getHeaders_returnsAllValues() {
        when(request.getHeaders("X-Multi"))
                .thenReturn(Collections.enumeration(Arrays.asList("a", "b")));

        Collection<String> values = headers.getHeaders("X-Multi");
        assertTrue(values.contains("a"));
        assertTrue(values.contains("b"));
    }

    @Test
    public void getHeaders_returnsEmptyForMissingHeader() {
        when(request.getHeaders("X-None")).thenReturn(null);

        Collection<String> values = headers.getHeaders("X-None");
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void containsHeader_trueWhenHeaderPresent() {
        when(request.getHeader("X-Present")).thenReturn("yes");

        assertTrue(headers.containsHeader("X-Present"));
    }

    @Test
    public void containsHeader_falseWhenHeaderAbsent() {
        when(request.getHeader("X-Absent")).thenReturn(null);

        assertFalse(headers.containsHeader("X-Absent"));
    }

    @Test
    public void getHeaderNames_includesSetHeaderNames() {
        when(request.getHeaderNames())
                .thenReturn(Collections.enumeration(Arrays.asList("X-A", "X-B")));

        Collection<String> names = headers.getHeaderNames();
        assertTrue(names.contains("X-A"));
        assertTrue(names.contains("X-B"));
    }
}
