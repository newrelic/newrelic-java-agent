/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.logging.IAgentLogger;

import java.util.logging.Level;

public class FakeAgentLogger implements IAgentLogger {
    @Override
    public void severe(String s) { }

    @Override
    public void error(String s) { }

    @Override
    public void warning(String s) { }

    @Override
    public void info(String s) { }

    @Override
    public void config(String s) { }

    @Override
    public void fine(String s) { }

    @Override
    public void finer(String s) { }

    @Override
    public void finest(String s) { }

    @Override
    public void debug(String s) { }

    @Override
    public void trace(String s) { }

    @Override
    public void severe(boolean allowForwarding, String s) {

    }

    @Override
    public void error(boolean allowForwarding, String s) {

    }

    @Override
    public void warning(boolean allowForwarding, String s) {

    }

    @Override
    public void info(boolean allowForwarding, String s) {

    }

    @Override
    public void config(boolean allowForwarding, String s) {

    }

    @Override
    public void fine(boolean allowForwarding, String s) {

    }

    @Override
    public void finer(boolean allowForwarding, String s) {

    }

    @Override
    public void finest(boolean allowForwarding, String s) {

    }

    @Override
    public void debug(boolean allowForwarding, String s) {

    }

    @Override
    public void trace(boolean allowForwarding, String s) {

    }

    @Override
    public boolean isFineEnabled() {
        return false;
    }

    @Override
    public boolean isFinerEnabled() {
        return false;
    }

    @Override
    public boolean isFinestEnabled() {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void log(Level level, String s, Throwable throwable) { }

    @Override
    public boolean isLoggable(Level level) {
        return false;
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object[] msg) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7, Object... otherParts) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object[] msg) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7, Object... otherParts) {

    }

    @Override
    public void logToChild(String childName, Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4) {

    }

    @Override
    public void log(Level level, String pattern, Object[] msg) { }

    @Override
    public void log(Level level, String s) { }

    @Override
    public void log(Level level, String pattern, Object part1) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7) { }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7,
            Object... otherParts) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object[] msg) { }

    @Override
    public void log(Level level, Throwable t, String pattern) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7) { }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7,
            Object... otherParts) { }

    @Override
    public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3, Object part4) { }

    @Override
    public void log(Level level, String s, Object[] objects, Throwable throwable) { }

    @Override
    public void log(Level level, boolean allowForwarding, String message, Throwable throwable) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String message) {

    }

    @Override
    public void log(Level level, boolean allowForwarding, String message, Object[] args, Throwable throwable) {

    }

    @Override
    public IAgentLogger getChildLogger(Class<?> aClass) {
        return null;
    }

    @Override
    public IAgentLogger getChildLogger(String s) {
        return null;
    }
}
