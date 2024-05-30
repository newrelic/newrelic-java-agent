package com.nr.agent.instrumentation.log4j1;

import org.junit.Test;

import static  org.junit.Assert.*;

public class Log4j1ExceptionUtilTest {
    @Test
    public void getErrorStack_withThrowable_generatesFullStacktrace() {
        assertFalse(Log4j1ExceptionUtil.getErrorStack(createTestException()).contains("caused by: java.lang.Exception: inner exception"));

        assertTrue(Log4j1ExceptionUtil.getErrorStack(createTestExceptionWithCausedBy()).contains("caused by: java.lang.Exception: inner exception"));
    }

    @Test
    public void getErrorStack_withNullThrowable_returnsNull() {
        assertNull(Log4j1ExceptionUtil.getErrorStack(null));
    }

    @Test
    public void getErrorMessage_withThrowable_returnsErrorMessage() {
        assertEquals("test exception", Log4j1ExceptionUtil.getErrorMessage(createTestException()));
    }

    @Test
    public void getErrorMessage_withNullThrowable_returnsNull() {
        assertNull(Log4j1ExceptionUtil.getErrorMessage(null));
    }

    @Test
    public void getErrorClass_withThrowable_returnsErrorMessage() {
        assertEquals("java.lang.Exception", Log4j1ExceptionUtil.getErrorClass(createTestException()));
    }

    @Test
    public void getErrorClass_withNullThrowable_returnsNull() {
        assertNull(Log4j1ExceptionUtil.getErrorClass(null));
    }

    private Exception createTestException() {
        return new Exception("test exception");
    }

    private Exception createTestExceptionWithCausedBy() {
        return new Exception("test exception", new Exception("inner exception"));
    }
}
