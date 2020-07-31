/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.api.agent.Logger;

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

    boolean isFineEnabled();

    boolean isFinerEnabled();

    boolean isFinestEnabled();

    boolean isDebugEnabled();

    boolean isTraceEnabled();

    void log(Level level, String message, Throwable throwable);

    @Override
    void log(Level level, String message);

    void log(Level level, String message, Object[] args, Throwable throwable);

    IAgentLogger getChildLogger(Class<?> clazz);

    IAgentLogger getChildLogger(String fullName);

}
