/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.log4j1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Log4j1ExceptionUtil {
    public static final int MAX_STACK_SIZE = 300;

    public static String getErrorStack(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable t = throwable;
        List<String> lines = new ArrayList<>();
        boolean inner = false;
        while (t != null && lines.size() < MAX_STACK_SIZE) {
            if (inner) {
                lines.add(" caused by: " + t.getClass().getName() + ": " + t.getMessage());
            }
            lines.addAll(stackTracesToStrings(t.getStackTrace()));
            t = t.equals(t.getCause()) ? null : t.getCause();
            inner = true;
        }

        return String.join("\n", lines.subList(0, Math.min(lines.size(), MAX_STACK_SIZE)));
    }

    public static String getErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getMessage();
    }

    public static String getErrorClass(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getClass().getName();
    }

    private static Collection<String> stackTracesToStrings(StackTraceElement[] stackTraces) {
        if (stackTraces == null || stackTraces.length == 0) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(stackTraces.length);
        for (StackTraceElement e : stackTraces) {
            lines.add("    at " + e.toString());
        }

        return lines;
    }
}