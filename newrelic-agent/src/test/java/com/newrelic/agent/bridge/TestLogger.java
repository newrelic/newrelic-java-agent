/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.api.agent.Logger;

public class TestLogger implements Logger {
    public final List<LogStatement> statements = new ArrayList<>();

    @Override
    public boolean isLoggable(Level level) {
        return true;
    }

    @Override
    public void log(Level level, String pattern, Object[] msg) {
        statements.add(new LogStatement(level, null, null, pattern, msg));
    }

    @Override
    public void log(Level level, String pattern) {
        statements.add(new LogStatement(level, null, null, pattern));
    }

    @Override
    public void log(Level level, String pattern, Object part1) {
        statements.add(new LogStatement(level, null, null, pattern, part1));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2) {
        statements.add(new LogStatement(level, null, null, pattern, part1, part2));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3) {
        statements.add(new LogStatement(level, null, null, pattern, part1, part2, part3));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
        statements.add(new LogStatement(level, null, null, pattern, part1, part2, part3, part4));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
        statements.add(new LogStatement(level, null, null, pattern, part1, part2, part3, part4, part5));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6) {
        statements.add(new LogStatement(level, null, null, pattern, part1, part2, part3, part4, part5, part6));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7) {
        statements.add(new LogStatement(level, null, null, pattern, part1, part2, part3, part4, part5, part6, part7));
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
                    Object part6, Object part7, Object... otherParts) {
        Object[] parts = merge(otherParts, part1, part2, part3, part4, part5, part6, part7);
        statements.add(new LogStatement(level, null, null, pattern, parts));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object[] msg) {
        statements.add(new LogStatement(level, null, t, pattern, msg));
    }

    @Override
    public void log(Level level, Throwable t, String pattern) {
        statements.add(new LogStatement(level, null, t, pattern));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1) {
        statements.add(new LogStatement(level, null, t, pattern, part1));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2) {
        statements.add(new LogStatement(level, null, t, pattern, part1, part2));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) {
        statements.add(new LogStatement(level, null, t, pattern, part1, part2, part3));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) {
        statements.add(new LogStatement(level, null, t, pattern, part1, part2, part3, part4));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5) {
        statements.add(new LogStatement(level, null, t, pattern, part1, part2, part3, part4, part5));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6) {
        statements.add(new LogStatement(level, null, t, pattern, part1, part2, part3, part4, part5, part6));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6, Object part7) {
        statements.add(new LogStatement(level, null, t, pattern, part1, part2, part3, part4, part5, part6, part7));
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
                    Object part5, Object part6, Object part7, Object... otherParts) {
        Object[] parts = merge(otherParts, part1, part2, part3, part4, part5, part6, part7);
        statements.add(new LogStatement(level, null, t, pattern, parts));
    }

    @Override
    public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3,
            Object part4) {
        statements.add(new LogStatement(level, childName, null, pattern, part1, part2, part3, part4));
    }

    private Object[] merge(Object[] otherParts, Object... firstParameters) {
        int otherPartsLength = otherParts != null ? otherParts.length : 0;
        Object[] mergedArray = new Object[firstParameters.length + otherPartsLength];

        System.arraycopy(firstParameters, 0, mergedArray, 0, firstParameters.length);
        if (otherPartsLength > 0) {
            System.arraycopy(otherParts, 0, mergedArray, mergedArray.length, otherPartsLength);
        }

        return mergedArray;
    }

    public static class LogStatement {

        public final Level level;
        public final Throwable throwable;
        public final String pattern;
        public final Object[] parts;
        public final String childName;

        public LogStatement(Level level, String child, Throwable throwable, String pattern, Object... parts) {
            this.level = level;
            this.throwable = throwable;
            this.pattern = pattern;
            this.parts = parts;
            this.childName = child;
        }

        @Override
        public String toString() {
            return "LogStatement [level=" + level + ", throwable=" + throwable + ", pattern=" + pattern + ", parts="
                    + Arrays.toString(parts) + ", childName=" + childName + "]";
        }

    }

}
