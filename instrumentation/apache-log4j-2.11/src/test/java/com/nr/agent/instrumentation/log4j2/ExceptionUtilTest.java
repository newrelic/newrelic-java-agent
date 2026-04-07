package com.nr.agent.instrumentation.log4j2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExceptionUtilTest {

    @Test
    public void getErrorStack_withThrowable_generatesFullStacktrace() {
        assertFalse(ExceptionUtil.getErrorStack(createNestedThrowable(1)).contains("caused by: java.lang.Throwable: Root Cause (Level 0)"));

        assertTrue(ExceptionUtil.getErrorStack(createNestedThrowable(5)).contains("caused by: java.lang.Throwable: Nested Exception (Level 1)"));
        assertTrue(ExceptionUtil.getErrorStack(createNestedThrowable(5)).contains("caused by: java.lang.Throwable: Nested Exception (Level 2)"));
        assertTrue(ExceptionUtil.getErrorStack(createNestedThrowable(5)).contains("caused by: java.lang.Throwable: Nested Exception (Level 3)"));
    }

    @Test
    public void getErrorStack_withLargeCausedByStack_truncatesStackTrace() {
        // This was causing an OOM prior to the fix
        assertEquals(300, ExceptionUtil.getErrorStack(createNestedThrowable(35000)).split("\n").length);
    }

    @Test
    public void getErrorStack_withNullThrowable_returnsNull() {
        assertNull(ExceptionUtil.getErrorStack(null));
    }

    @Test
    public void testIsThrowableNull() {
        Throwable nullThrowable = null;
        Throwable nonNullThrowable = new Throwable("Hi");

        assertTrue(ExceptionUtil.isThrowableNull(nullThrowable));
        assertFalse(ExceptionUtil.isThrowableNull(nonNullThrowable));
    }

    @Test
    public void getErrorMessage_withThrowable_returnsErrorMessage() {
        assertEquals("Root Cause (Level 0)", ExceptionUtil.getErrorMessage(createNestedThrowable(1)));
    }

    @Test
    public void getErrorMessage_withNullThrowable_returnsNull() {
        assertNull(ExceptionUtil.getErrorMessage(null));
    }

    @Test
    public void getErrorClass_withThrowable_returnsErrorMessage() {
        assertEquals("java.lang.Throwable", ExceptionUtil.getErrorClass(createNestedThrowable(1)));
    }

    @Test
    public void getErrorClass_withNullThrowable_returnsNull() {
        assertNull(ExceptionUtil.getErrorClass(null));
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
