/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.api.agent.Logger;

import java.text.MessageFormat;
import java.util.logging.Level;

public interface IAgentLogger extends Logger {

    void severe(String message);

    void error(String message);

    void warning(String message);

    void info(String message);

    void config(String message);

    void fine(String message);

    void finer(String message);

    void finest(String message);

    void debug(String message);

    void trace(String message);

    void severe(boolean allowForwarding, String message);

    void error(boolean allowForwarding, String message);

    void warning(boolean allowForwarding, String message);

    void info(boolean allowForwarding, String message);

    void config(boolean allowForwarding, String message);

    void fine(boolean allowForwarding, String message);

    void finer(boolean allowForwarding, String message);

    void finest(boolean allowForwarding, String message);

    void debug(boolean allowForwarding, String message);

    void trace(boolean allowForwarding, String message);

    boolean isFineEnabled();

    boolean isFinerEnabled();

    boolean isFinestEnabled();

    boolean isDebugEnabled();

    boolean isTraceEnabled();

    void log(Level level, String message, Throwable throwable);

    @Override
    void log(Level level, String message);

    void log(Level level, String message, Object[] args, Throwable throwable);

    void log(Level level, boolean allowForwarding, String message, Throwable throwable);

    void log(Level level, boolean allowForwarding, String message);

    void log(Level level, boolean allowForwarding, String message, Object[] args, Throwable throwable);

    IAgentLogger getChildLogger(Class<?> clazz);

    IAgentLogger getChildLogger(String fullName);

    /**
     * Do not use. This exists only for legacy reasons.
     *
     * @param level The level at which the message should be logged.
     * @param allowForwarding A boolean flag that indicates if the agent log can be forwarded.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param msg The parts to be placed in the log message using the {@link MessageFormat} style.
     */
    void log(Level level, boolean allowForwarding, String pattern, Object[] msg);

    void log(Level level, boolean allowForwarding, String pattern, Object part1);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7);

    void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7, Object... otherParts);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object[] msg);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7);

    void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7, Object... otherParts);

    void logToChild(String childName, Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4);

}
