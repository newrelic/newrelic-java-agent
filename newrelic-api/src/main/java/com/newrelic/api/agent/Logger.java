/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * The Java agent's logging interface. Use this interface to create entries in the Agent's log. By default, the Agent's
 * log is written to the file newrelic_agent.log in the logs/ subdirectory at the Agent's install location.
 */
public interface Logger {

    /**
     * Returns true if the given log level will be logged. Generally this method should NOT be used - just call the
     * {@link #log(Level, String, Object...)} methods with the message broken into parts. The overhead of the
     * concatenation will not be incurred if the log level isn't met.
     * 
     * @param level The level to be verified.
     * @return True if a message could be logged at the given level, else false.
     * @since 3.9.0
     */
    boolean isLoggable(Level level);


    /**
     * Do not use. This exists only for legacy reasons.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param msg The parts to be placed in the log message using the {@link MessageFormat} style.
     */
    void log(Level level, String pattern, Object[] msg);

    /**
     * Concatenate the given parts and log them at the given level. If a part is <code>null</code>, its value will be
     * represented as "null". If a part is a <code>Class</code>, the value of {@link Class#getName()} will be used.
     * 
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @since 3.9.0
     */
    void log(Level level, String pattern);

    /**
     * Log a message at the given level using the provided pattern with one replacement.
     * 
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1);

    /**
     * Log a message at the given level using the provided pattern with two replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2);

    /**
     * Log a message at the given level using the provided pattern with three replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2, Object part3);

    /**
     * Log a message at the given level using the provided pattern with four replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4);

    /**
     * Log a message at the given level using the provided pattern with five replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5);

    /**
     * Log a message at the given level using the provided pattern with six replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part6 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6);

    /**
     * Log a message at the given level using the provided pattern with seven replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part6 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part7 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7);

    /**
     * Log a message at the given level using the provided pattern with seven + any other replacements.
     *
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part6 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part7 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param otherParts The other parts to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7, Object... otherParts);

    /**
     * Do not use. This exists only for legacy reasons.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param msg The parts to be placed in the log message using the {@link MessageFormat} style.
     */
    void log(Level level, Throwable t, String pattern, Object[] msg);

    /**
     * Log a message with given Throwable information. Concatenate the given msg and log them at the given level. If a
     * msg is <code>null</code>, its value will be represented as "null". If a part is a
     * <code>Class</code>, the value of {@link Class#getName()} will be used.
     * 
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @since 3.9.0
     */
    void log(Level level, Throwable t, String pattern);

    /**
     * Log a message at the given level using the provided pattern and throwable with one replacement.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1);

    /**
     * Log a message at the given level using the provided pattern and throwable with two replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2);

    /**
     * Log a message at the given level using the provided pattern and throwable with three replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3);

    /**
     * Log a message at the given level using the provided pattern and throwable with four replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4);

    /**
     * Log a message at the given level using the provided pattern and throwable with five replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5);

    /**
     * Log a message at the given level using the provided pattern and throwable with six replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part6 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6);

    /**
     * Log a message at the given level using the provided pattern and throwable with seven replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part6 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part7 The part to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7);

    /**
     * Log a message at the given level using the provided pattern and throwable with seven + any other replacements.
     *
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part5 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part6 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part7 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param otherParts The other parts to be replaced in the log message using the {@link MessageFormat} style
     */
    void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7, Object... otherParts);

    /**
     * Concatenate the given parts and log them at the given level. If a part is <code>null</code>, its value will be
     * represented as "null". If a part is a <code>Class</code>, the value of {@link Class#getName()} will be used.
     * 
     * @param childName The name of the child logger.
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param part1 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part2 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part3 The part to be replaced in the log message using the {@link MessageFormat} style
     * @param part4 The part to be replaced in the log message using the {@link MessageFormat} style
     * @since 3.9.0
     */
    void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3, Object part4);
}
