/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.sling;

import jakarta.servlet.ServletOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

// Test creation was AI assisted
public class NewRelicOutputStreamWrapperTest {
    private ServletOutputStream mockStream;
    private NewRelicOutputStreamWrapper wrapperForMockStream;

    @Before
    public void setup() {
        mockStream = mock(ServletOutputStream.class);
        wrapperForMockStream = new NewRelicOutputStreamWrapper(mockStream);
    }

    @Test
    public void write_withoutHeadTag_neverSetsInjectedFlag() throws IOException {
        // All content passes through immediately; hasInjected stays false when no <head> appears.
        wrapperForMockStream.write("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz".getBytes(StandardCharsets.ISO_8859_1));
        assertFalse(wrapperForMockStream.hasInjected());

        setup();
        byte[] bytes = "12345abc12345".getBytes(StandardCharsets.ISO_8859_1);
        wrapperForMockStream.write(bytes, 5, 3);
        assertFalse(wrapperForMockStream.hasInjected());

        setup();
        wrapperForMockStream.write(65); // 'A'
        assertFalse(wrapperForMockStream.hasInjected());
    }

    @Test
    public void write_withHeadTag_setsInjectedFlagToTrue() throws IOException {
        wrapperForMockStream.write("zzzzzzzzzzzzz<head>zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz".getBytes(StandardCharsets.ISO_8859_1));
        verify(mockStream, atLeastOnce()).write(any(byte[].class));
        assertTrue(wrapperForMockStream.hasInjected());

        setup();
        byte[] bytes = "12345<head>c12345".getBytes(StandardCharsets.ISO_8859_1);
        wrapperForMockStream.write(bytes, 5, 12); // "<head>c12345"
        verify(mockStream, atLeastOnce()).write(any(byte[].class));
        assertTrue(wrapperForMockStream.hasInjected());
    }

    @Test
    public void flush_delegatesToUnderlyingStream() throws IOException {
        // Content passes through during write; flush just delegates
        wrapperForMockStream.write("some content".getBytes(StandardCharsets.ISO_8859_1));
        wrapperForMockStream.flush();
        verify(mockStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
        verify(mockStream).flush();
        // flush() does not change hasInjected — no <head> was written
        assertFalse(wrapperForMockStream.hasInjected());

        // Already injected — flush only delegates, no extra write
        setup();
        wrapperForMockStream.write("before<head>after".getBytes(StandardCharsets.ISO_8859_1)); // triggers injection
        reset(mockStream);
        wrapperForMockStream.flush();
        verify(mockStream, never()).write(any(byte[].class));
        verify(mockStream, never()).write(any(byte[].class), anyInt(), anyInt());
        verify(mockStream).flush();

        // Nothing written yet — flush still delegates
        setup();
        wrapperForMockStream.flush();
        verify(mockStream, never()).write(any(byte[].class));
        verify(mockStream).flush();
    }

    @Test
    public void close_delegatesToUnderlyingStream() throws IOException {
        // Content passes through during write; close just delegates
        wrapperForMockStream.write("some content".getBytes(StandardCharsets.ISO_8859_1));
        wrapperForMockStream.close();
        verify(mockStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
        verify(mockStream).close();
        assertFalse(wrapperForMockStream.hasInjected());

        // Already injected — close only delegates, no extra write
        setup();
        wrapperForMockStream.write("before<head>after".getBytes(StandardCharsets.ISO_8859_1)); // triggers injection
        reset(mockStream);
        wrapperForMockStream.close();
        verify(mockStream, never()).write(any(byte[].class));
        verify(mockStream, never()).write(any(byte[].class), anyInt(), anyInt());
        verify(mockStream).close();

        // Nothing written yet — close still delegates
        setup();
        wrapperForMockStream.close();
        verify(mockStream, never()).write(any(byte[].class));
        verify(mockStream).close();
    }
}