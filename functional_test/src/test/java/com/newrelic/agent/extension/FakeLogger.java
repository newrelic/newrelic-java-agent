/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.api.agent.Logger;

import java.util.logging.Level;

public class FakeLogger implements Logger {
    Level recordedLevel = null;
    String recordedString = null;

    @Override
    public void log(Level level, String pattern) {
        recordedLevel = level;
        recordedString = pattern;
    }

    @Override
    public boolean isLoggable(Level level) {
        return false;
    }

    @Override
    public void log(Level level, String pattern, Object[] msg) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7) { throw new RuntimeException(); }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7,
            Object... otherParts) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object[] msg) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7) { throw new RuntimeException(); }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7, Object... otherParts) { throw new RuntimeException(); }

    @Override
    public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3, Object part4) { throw new RuntimeException(); }
}
