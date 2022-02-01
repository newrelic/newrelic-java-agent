/*
 * Copyright 2019. New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nr.agent.instrumentation.logbackclassic12;

public class ExceptionUtil {
    public static final int MAX_STACK_SIZE = 1000; // TODO figure out size limit

    public static String getErrorStack(Throwable throwable) {
        if (throwable == null) {
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
        for (int i = 0; i < Math.min(maxStackSize, stack.length); i++) {
            stackBuilder.append("  at ").append(stack[i].toString()).append("\n");
        }
        return stackBuilder.toString();
    }
}
