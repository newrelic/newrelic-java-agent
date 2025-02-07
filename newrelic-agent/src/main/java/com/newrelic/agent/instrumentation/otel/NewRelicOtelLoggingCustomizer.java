/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.otel;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class NewRelicOtelLoggingCustomizer implements LoggingCustomizer {

    private static final Map<String, LogAdapter> CACHE = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "nr";
    }

    @Override
    public void init(EarlyInitAgentConfig earlyInitAgentConfig) {
        InternalLogger.initialize(
                name -> CACHE.computeIfAbsent(name, LogAdapter::new)
        );
    }

    @Override
    public void onStartupSuccess() {
    }

    @Override
    public void onStartupFailure(Throwable throwable) {
    }

    private static class LogAdapter implements InternalLogger {

        private static final Map<Level, java.util.logging.Level> LEVEL_MAP;

        static {
            LEVEL_MAP = new EnumMap<>(Level.class);
            LEVEL_MAP.put(Level.DEBUG, java.util.logging.Level.FINEST);
            LEVEL_MAP.put(Level.TRACE, java.util.logging.Level.FINER);
            LEVEL_MAP.put(Level.INFO, java.util.logging.Level.INFO);
            LEVEL_MAP.put(Level.WARN, java.util.logging.Level.WARNING);
            LEVEL_MAP.put(Level.ERROR, java.util.logging.Level.SEVERE);
        };

        private final IAgentLogger logger;
        private final String name;

        private LogAdapter(String name) {
            this.name = name;
            this.logger = Agent.LOG.getChildLogger(name);
        }

        @Override
        public boolean isLoggable(Level level) {
            return logger.isLoggable(LEVEL_MAP.get(level));
        }

        @Override
        public void log(Level level, String message, @Nullable Throwable throwable) {
            logger.log(LEVEL_MAP.get(level), message, throwable);
        }

        @Override
        public String name() {
            return name;
        }
    }

}
