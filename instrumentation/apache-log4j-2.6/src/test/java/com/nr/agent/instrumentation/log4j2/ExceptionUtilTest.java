package com.nr.agent.instrumentation.log4j2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExceptionUtilTest {

    @Test
    public void testIsThrowableNull() {
        Throwable nullThrowable = null;
        Throwable nonNullThrowable = new Throwable("Hi");

        assertTrue(ExceptionUtil.isThrowableNull(nullThrowable));
        assertFalse(ExceptionUtil.isThrowableNull(nonNullThrowable));
    }

    @Test
    public void testGetErrorStack() {
        int maxStackSize = 3;
        StackTraceElement stackTraceElement1 = new StackTraceElement("Class1", "method1", "File1", 1);
        StackTraceElement stackTraceElement2 = new StackTraceElement("Class2", "method2", "File2", 2);
        StackTraceElement stackTraceElement3 = new StackTraceElement("Class3", "method3", "File3", 3);
        StackTraceElement stackTraceElement4 = new StackTraceElement("Class4", "method4", "File4", 4);
        StackTraceElement stackTraceElement5 = new StackTraceElement("Class5", "method5", "File5", 5);
        StackTraceElement[] stack = new StackTraceElement[] { stackTraceElement1, stackTraceElement2, stackTraceElement3, stackTraceElement4,
                stackTraceElement5 };
        String errorStack = ExceptionUtil.getErrorStack(stack, maxStackSize);

        // Processed stack should be limited to only the first three lines
        assertTrue(errorStack.contains(stackTraceElement1.toString()));
        assertTrue(errorStack.contains(stackTraceElement2.toString()));
        assertTrue(errorStack.contains(stackTraceElement3.toString()));
        // Processed stack should omit the last two lines
        assertFalse(errorStack.contains(stackTraceElement4.toString()));
        assertFalse(errorStack.contains(stackTraceElement5.toString()));
    }

    @Test
    public void testGetErrorMessage() {
        String expectedMessage = "Hi";
        Throwable nullThrowable = null;
        Throwable nonNullThrowable = new Throwable(expectedMessage);

        assertNull(ExceptionUtil.getErrorMessage(nullThrowable));
        assertEquals(expectedMessage, ExceptionUtil.getErrorMessage(nonNullThrowable));
    }

    @Test
    public void testGetErrorClass() {
        String expectedExceptionClass = "java.lang.RuntimeException";
        Throwable nullThrowable = null;
        RuntimeException runtimeException = new RuntimeException("Hi");

        assertNull(ExceptionUtil.getErrorClass(nullThrowable));
        assertEquals(expectedExceptionClass, ExceptionUtil.getErrorClass(runtimeException));
    }
}
