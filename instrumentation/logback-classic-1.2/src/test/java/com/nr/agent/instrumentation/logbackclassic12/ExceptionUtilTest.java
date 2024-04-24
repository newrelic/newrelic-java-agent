package com.nr.agent.instrumentation.logbackclassic12;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExceptionUtilTest {

    @Test
    public void getErrorStack_withThrowable_generatesFullStacktrace() {
        assertFalse(ExceptionUtil.getErrorStack(createTestException()).contains("caused by: java.lang.Exception: inner exception"));

        assertTrue(ExceptionUtil.getErrorStack(createTestExceptionWithCausedBy()).contains("caused by: java.lang.Exception: inner exception"));
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
        assertEquals("test exception", ExceptionUtil.getErrorMessage(createTestException()));
    }

    @Test
    public void getErrorMessage_withNullThrowable_returnsNull() {
        assertNull(ExceptionUtil.getErrorMessage(null));
    }

    @Test
    public void getErrorClass_withThrowable_returnsErrorMessage() {
        assertEquals("java.lang.Exception", ExceptionUtil.getErrorClass(createTestException()));
    }

    @Test
    public void getErrorClass_withNullThrowable_returnsNull() {
        assertNull(ExceptionUtil.getErrorClass(null));
    }

    private Exception createTestException() {
        return new Exception("test exception");
    }

    private Exception createTestExceptionWithCausedBy() {
        return new Exception("test exception", new Exception("inner exception"));
    }
}
