/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Logger;

import java.util.logging.Level;

class NoOpLogger implements Logger {

    static final Logger INSTANCE = new NoOpLogger();

    private NoOpLogger() {
    }

    @Override
    public boolean isLoggable(Level level) {
        return false;
    }

    @Override
    public void log(Level level, String pattern, Object[] msg) {
    }

    @Override
    public void log(Level level, String pattern) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern) {
    }

    @Override
    public void log(Level level, String pattern, Object part1) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7) {
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
                    Object part6, Object part7, Object... otherParts) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object[] msg) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6, Object part7) {
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
                    Object part5, Object part6, Object part7, Object... otherParts) {
    }

    @Override
    public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3,
            Object part4) {
    }
}
