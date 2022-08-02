package com.nr.agent.instrumentation.log4j2;

public class ExceptionUtil {
    public static final int MAX_STACK_SIZE = 300;

    public static boolean isThrowableNull(Throwable throwable) {
        return throwable == null;
    }

    public static String getErrorStack(Throwable throwable) {
        if (isThrowableNull(throwable)) {
            return null;
        }

        StackTraceElement[] stack = throwable.getStackTrace();
        return getErrorStack(stack);
    }

    public static String getErrorStack(StackTraceElement[] stack) {
        return getErrorStack(stack, MAX_STACK_SIZE);
    }

    public static String getErrorStack(StackTraceElement[] stack, Integer maxStackSize) {
        if (stack == null || stack.length == 0) {
            return null;
        }

        StringBuilder stackBuilder = new StringBuilder();
        int stackSizeLimit = Math.min(maxStackSize, stack.length);
        for (int i = 0; i < stackSizeLimit; i++) {
            stackBuilder.append("  at ").append(stack[i].toString()).append("\n");
        }
        return stackBuilder.toString();
    }

    public static String getErrorMessage(Throwable throwable) {
        if (isThrowableNull(throwable)) {
            return null;
        }
        return throwable.getMessage();
    }

    public static String getErrorClass(Throwable throwable) {
        if (isThrowableNull(throwable)) {
            return null;
        }
        return throwable.getClass().getName();
    }
}
