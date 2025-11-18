/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.logs;

public class ExceptionUtil {
    public static final int MAX_STACK_SIZE = 300;

    public static String getErrorStack(String errorStack) {
        if (validateString(errorStack) == null) {
            return null;
        }
        if (errorStack.length() <= MAX_STACK_SIZE) {
            return errorStack;
        }
        return errorStack.substring(0, MAX_STACK_SIZE);
    }

    public static String getErrorMessage(String errorMessage) {
        return validateString(errorMessage);
    }

    public static String getErrorClass(String errorClass) {
        return validateString(errorClass);
    }

    public static String validateString(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }
}
