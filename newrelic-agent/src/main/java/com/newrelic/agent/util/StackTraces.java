/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.service.ServiceFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StackTraces {
    private StackTraces() {
    }

    public static StackTraceElement[] getThreadStackTraceElements(long threadId) {
        ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(threadId, Integer.MAX_VALUE);
        if (threadInfo == null) {
            return null;
        }
        return threadInfo.getStackTrace();
    }

    public static Exception createStackTraceException(String message) {
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        return createStackTraceException(message, stackTraces, true);
    }

    public static Exception createStackTraceException(String message, StackTraceElement[] stackTraces, boolean scrub) {
        return createStackTraceException(new Exception(message), stackTraces, scrub);
    }

    public static Exception createStackTraceException(Exception e, StackTraceElement[] stackTraces, boolean scrub) {
        List<StackTraceElement> scrubbedTrace = scrub ? scrubAndTruncate(stackTraces) : Arrays.asList(stackTraces);

        e.setStackTrace(scrubbedTrace.toArray(new StackTraceElement[0]));
        return e;
    }

    public static List<String> toStringList(List<StackTraceElement> stackElements) {
        List<String> stringList = new ArrayList<>(stackElements.size());
        for (StackTraceElement element : stackElements) {
            stringList.add(element.toString());
        }
        return stringList;
    }

    public static List<String> toStringListRemoveParent(List<StackTraceElement> stackElements,
            List<StackTraceElement> parentBacktrace) {
        if (parentBacktrace == null || (parentBacktrace.size() <= 1)) {
            return toStringList(stackElements);
        } else {
            // need to tear off to parent
            parentBacktrace = StackTraces.scrubAndTruncate(parentBacktrace);
            // make sure we did not get rid of every line
            if (parentBacktrace == null || (parentBacktrace.size() <= 1)) {
                return toStringList(stackElements);
            }

            // we should class/method match the first and exactly match the second
            StackTraceElement parentLatestFirst = parentBacktrace.get(0);
            StackTraceElement parentLatestSecond = parentBacktrace.get(1);
            List<String> stringList = new ArrayList<>();
            int currentLength = stackElements.size();
            StackTraceElement current;
            for (int i = 0; i < currentLength; i++) {
                current = stackElements.get(i);

                // 1 - the parentLatestFirst should match the class and method
                // 2 - the next should exist and should equal the parentLatestSecond
                if (isSameClassAndMethod(current, parentLatestFirst) && ((i + 1) < currentLength)
                        && (stackElements.get(i + 1).equals(parentLatestSecond))) {
                    break;
                } else {
                    stringList.add(current.toString());
                }
            }

            return stringList;
        }
    }

    protected static boolean isSameClassAndMethod(StackTraceElement one, StackTraceElement two) {
        if (one == two) {
            return true;
        } else {
            return (one.getClassName().equals(two.getClassName()) && (one.getMethodName().equals(two.getMethodName())));
        }
    }

    public static List<StackTraceElement> scrubAndTruncate(StackTraceElement[] stackTraces) {
        return scrubAndTruncate(Arrays.asList(stackTraces));
    }

    /**
     * Returns a truncated list of stack trace elements that has been scrubbed of New Relic class references. The list
     * is modifiable.
     * 
     * @param stackTraces
     */
    public static List<StackTraceElement> scrubAndTruncate(List<StackTraceElement> stackTraces) {
        return scrubAndTruncate(stackTraces,
                ServiceFactory.getConfigService().getDefaultAgentConfig().getMaxStackTraceLines());
    }

    /**
     * Returns a list of stack trace elements that has been scrubbed of New Relic class references.
     * 
     * @param stackTraces
     * @param maxStackTraceLines truncate the list to the given number of lines. if 0, don't truncate
     * @see #truncateStack(List, int)
     */
    public static List<StackTraceElement> scrubAndTruncate(List<StackTraceElement> stackTraces, int maxStackTraceLines) {
        List<StackTraceElement> trimmedList = scrub(stackTraces);
        return maxStackTraceLines > 0 ? truncateStack(trimmedList, maxStackTraceLines) : trimmedList;
    }

    public static List<StackTraceElement> scrub(List<StackTraceElement> stackTraces) {
        for (int i = stackTraces.size() - 1; i >= 0; i--) {
            StackTraceElement element = stackTraces.get(i);
            // we need the dot - data services has some packages that start with com.newrelic.agentvalidator
            if (element.getClassName().startsWith("com.newrelic.agent.")
                    || element.getClassName().startsWith("com.newrelic.bootstrap.")
                    || element.getClassName().startsWith("com.newrelic.api.agent.")
                    || element.getClassName().startsWith("com.newrelic.weave.")
                    || element.getClassName().startsWith("com.nr.agent.instrumentation.")
                    || ("getAgentHandle".equals(element.getMethodName()) && "java.lang.reflect.Proxy".equals(element.getClassName()))) {
                return stackTraces.subList(i + 1, stackTraces.size());
            }
        }
        return stackTraces;
    }

    public static List<StackTraceElement> last(StackTraceElement[] elements, int count) {
        List<StackTraceElement> list = Arrays.asList(elements);
        if (list.size() <= count) {
            return list;
        } else {
            return list.subList(list.size() - count, list.size());
        }
    }

    /**
     * @param elements
     * @param maxDepth
     * @return returns a list of stack trace elements that has been truncated
     */
    static List<StackTraceElement> truncateStack(List<StackTraceElement> elements, int maxDepth) {
        if (elements.size() <= maxDepth) {
            return elements;
        }

        int bottomLimit = Double.valueOf(Math.floor(maxDepth / 3)).intValue();
        int topLimit = maxDepth - bottomLimit;

        List<StackTraceElement> topStack = elements.subList(0, topLimit);
        List<StackTraceElement> bottomStack = elements.subList(elements.size() - bottomLimit, elements.size());
        int skipCount = elements.size() - bottomLimit - topLimit;

        // need to add one for the skipping line
        elements = new ArrayList<>(maxDepth + 1);
        elements.addAll(topStack);
        elements.add(new StackTraceElement("Skipping " + skipCount + " lines...", "", "", 0));
        elements.addAll(bottomStack);

        return elements;
    }

    public static Throwable getRootCause(Throwable throwable) {
        return throwable.getCause() == null ? throwable : throwable.getCause();
    }

    public static Collection<String> stackTracesToStrings(StackTraceElement[] stackTraces) {
        if (stackTraces == null || stackTraces.length == 0) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(stackTraces.length);
        for (StackTraceElement e : stackTraces) {
            lines.add('\t' + e.toString());
        }

        return lines;
    }

    public static boolean isInAgentInstrumentation(StackTraceElement[] stackTrace) {
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("com.newrelic.agent.")
                    || element.getClassName().startsWith("com.newrelic.weave.")) {
                return true;
            }
        }
        return false;
    }

}
