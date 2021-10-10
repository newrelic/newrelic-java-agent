/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This base implementation of a metric aggregator verifies input and catches and logs all exceptions thrown by the
 * classes implementing the main logic.
 */
public abstract class AbstractMetricAggregator implements MetricAggregator {

    private final Logger logger;

    protected AbstractMetricAggregator() {
        this(Agent.LOG);
    }

    protected AbstractMetricAggregator(Logger logger) {
        this.logger = logger;
    }

    @Override
    public final void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        if (name == null || name.length() == 0) {
            logger.log(Level.FINER, "recordResponseTimeMetric was invoked with a null or empty name");
            return;
        }
        try {
            doRecordResponseTimeMetric(name, totalTime, exclusiveTime, timeUnit);
            logger.log(Level.FINER, "Recorded response time metric \"{0}\": {1}", name, totalTime);
        } catch (Throwable t) {
            logException(logger, t, "Exception recording response time metric \"{0}\": {1}", name, t);
        }
    }

    protected abstract void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime,
            TimeUnit timeUnit);

    @Override
    public final void recordMetric(String name, float value) {
        if (name == null || name.length() == 0) {
            logger.log(Level.FINER, "recordMetric was invoked with a null or empty name");
            return;
        }
        try {
            doRecordMetric(name, value);
            if (Agent.isDebugEnabled()) {
                logger.log(Level.FINER, "Recorded metric \"{0}\": {1}", name, value);
            }
        } catch (Throwable t) {
            logException(logger, t, "Exception recording metric \"{0}\": {1}", name, t);
        }
    }

    protected abstract void doRecordMetric(String name, float value);

    @Override
    public final void recordResponseTimeMetric(String name, long millis) {
        recordResponseTimeMetric(name, millis, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public final void incrementCounter(String name) {
        incrementCounter(name, 1);
    }

    @Override
    public final void incrementCounter(String name, int count) {
        if (name == null || name.length() == 0) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "incrementCounter was invoked with a null metric name");
            }
            return;
        }
        try {
            doIncrementCounter(name, count);
            if (Agent.isDebugEnabled()) {
                logger.log(Level.FINER, "incremented counter \"{0}\": {1}", name, count);
            }
        } catch (Throwable t) {
            logException(logger, t, "Exception incrementing counter \"{0}\",{1} : {2}", name, count, t);
        }
    }

    protected abstract void doIncrementCounter(String name, int count);

    private static void logException(Logger logger, Throwable t, String pattern, Object part1, Object part2) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, t, pattern, part1, part2);
        } else if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, pattern, part1, part2);
        }
    }

    private static void logException(Logger logger, Throwable t, String pattern, Object part1, Object part2, Object part3) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, t, pattern, part1, part2, part3);
        } else if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, pattern, part1, part2, part3);
        }
    }
}
