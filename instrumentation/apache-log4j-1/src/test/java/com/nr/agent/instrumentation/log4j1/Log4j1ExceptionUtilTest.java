package com.nr.agent.instrumentation.log4j1;

import org.junit.Test;

import static  org.junit.Assert.*;

public class Log4j1ExceptionUtilTest {
    @Test
    public void getErrorStack_withThrowable_generatesFullStacktrace() {
        assertFalse(Log4j1ExceptionUtil.getErrorStack(createNestedThrowable(1)).contains("caused by: java.lang.Throwable: Root Cause (Level 0)"));

        assertTrue(Log4j1ExceptionUtil.getErrorStack(createNestedThrowable(5)).contains("caused by: java.lang.Throwable: Nested Exception (Level 1)"));
        assertTrue(Log4j1ExceptionUtil.getErrorStack(createNestedThrowable(5)).contains("caused by: java.lang.Throwable: Nested Exception (Level 2)"));
        assertTrue(Log4j1ExceptionUtil.getErrorStack(createNestedThrowable(5)).contains("caused by: java.lang.Throwable: Nested Exception (Level 3)"));
    }

    @Test
    public void getErrorStack_withLargeCausedByStack_truncatesStackTrace() {
        // This was causing an OOM prior to the fix
        assertEquals(300, Log4j1ExceptionUtil.getErrorStack(createNestedThrowable(35000)).split("\n").length);
    }

    @Test
    public void getErrorStack_withNullThrowable_returnsNull() {
        assertNull(Log4j1ExceptionUtil.getErrorStack(null));
    }

    @Test
    public void getErrorMessage_withThrowable_returnsErrorMessage() {
        assertEquals("Root Cause (Level 0)", Log4j1ExceptionUtil.getErrorMessage(createNestedThrowable(1)));
    }

    @Test
    public void getErrorMessage_withNullThrowable_returnsNull() {
        assertNull(Log4j1ExceptionUtil.getErrorMessage(null));
    }

    @Test
    public void getErrorClass_withThrowable_returnsErrorMessage() {
        assertEquals("java.lang.Throwable", Log4j1ExceptionUtil.getErrorClass(createNestedThrowable(1)));
    }

    @Test
    public void getErrorClass_withNullThrowable_returnsNull() {
        assertNull(Log4j1ExceptionUtil.getErrorClass(null));
    }

    private static Throwable createNestedThrowable(int depth) {
        Throwable rootCause = new Throwable("Root Cause (Level " + (depth - 1) + ")");
        Throwable currentThrowable = rootCause;

        for (int i = depth - 2; i >= 0; i--) {
            currentThrowable = new Throwable("Nested Exception (Level " + i + ")", currentThrowable);
        }

        return currentThrowable;
    }
}
