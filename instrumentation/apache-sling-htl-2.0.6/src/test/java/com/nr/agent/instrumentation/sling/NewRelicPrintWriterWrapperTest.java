/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.sling;

import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class NewRelicPrintWriterWrapperTest {
    private PrintWriter mockWriter;
    private NewRelicPrintWriterWrapper wrapperForMockWriter;

    @Before
    public void setup() {
        mockWriter = mock(PrintWriter.class);
        wrapperForMockWriter = new NewRelicPrintWriterWrapper(mockWriter);
    }

    @Test
    public void write_withoutHeadTag_neverSetsInjectedFlag() {
        // All content passes through to the underlying writer immediately;
        // hasInjected stays false as long as no <head> tag appears.
        wrapperForMockWriter.write("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        assertFalse(wrapperForMockWriter.hasInjected());

        setup();
        wrapperForMockWriter.write("12345abc12345", 5, 3);
        assertFalse(wrapperForMockWriter.hasInjected());

        setup();
        char[] chars = {'a', 'b', 'c', 'd', 'e', 'f', 'h', 'e', 'a', 'd'};
        wrapperForMockWriter.write(chars); // "abcdefhead" — no closing '>' so no <head> match
        assertFalse(wrapperForMockWriter.hasInjected());

        setup();
        wrapperForMockWriter.write(chars, 1, 2);
        assertFalse(wrapperForMockWriter.hasInjected());

        setup();
        wrapperForMockWriter.write(65);
        assertFalse(wrapperForMockWriter.hasInjected());
    }

    @Test
    public void write_withHeadTag_setsInjectedFlagToTrue() {
        wrapperForMockWriter.write("zzzzzzzzzzzzz<head>zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        verify(mockWriter, atLeastOnce()).write(anyString());
        assertTrue(wrapperForMockWriter.hasInjected());

        setup();
        wrapperForMockWriter.write("12345<head>c12345", 5, 12); // "<head>c12345"
        verify(mockWriter, atLeastOnce()).write(anyString());
        assertTrue(wrapperForMockWriter.hasInjected());

        setup();
        char[] chars = {'a', 'b', 'c', 'd', 'e', 'f', '<', 'h', 'e', 'a', 'd', '>'};
        wrapperForMockWriter.write(chars);
        verify(mockWriter, atLeastOnce()).write("abcdef<head>");
        assertTrue(wrapperForMockWriter.hasInjected());

        setup();
        wrapperForMockWriter.write(chars, 0, 12);
        verify(mockWriter, atLeastOnce()).write("abcdef<head>");
        assertTrue(wrapperForMockWriter.hasInjected());
    }

    @Test
    public void flush_delegatesToUnderlyingWriter() {
        // Content passes through during write; flush just delegates
        wrapperForMockWriter.write("some content");
        wrapperForMockWriter.flush();
        verify(mockWriter, atLeastOnce()).write(anyString());
        verify(mockWriter).flush();
        // flush() does not change hasInjected — no <head> was written
        assertFalse(wrapperForMockWriter.hasInjected());

        // Already injected — flush only delegates, no extra write
        setup();
        wrapperForMockWriter.write("before<head>after");
        reset(mockWriter);
        wrapperForMockWriter.flush();
        verify(mockWriter, never()).write(anyString());
        verify(mockWriter).flush();

        // Nothing written yet — flush still delegates
        setup();
        wrapperForMockWriter.flush();
        verify(mockWriter, never()).write(anyString());
        verify(mockWriter).flush();
    }

    @Test
    public void close_delegatesToUnderlyingWriter() {
        // Content passes through during write; close just delegates
        wrapperForMockWriter.write("some content");
        wrapperForMockWriter.close();
        verify(mockWriter, atLeastOnce()).write(anyString());
        verify(mockWriter).close();
        assertFalse(wrapperForMockWriter.hasInjected());

        // Already injected — close only delegates, no extra write
        setup();
        wrapperForMockWriter.write("before<head>after");
        reset(mockWriter);
        wrapperForMockWriter.close();
        verify(mockWriter, never()).write(anyString());
        verify(mockWriter).close();

        // Nothing written yet — close still delegates
        setup();
        wrapperForMockWriter.close();
        verify(mockWriter, never()).write(anyString());
        verify(mockWriter).close();
    }
}