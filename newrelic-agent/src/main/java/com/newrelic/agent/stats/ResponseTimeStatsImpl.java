/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.util.TimeConversion;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class is not thread-safe.
 */
public class ResponseTimeStatsImpl extends AbstractStats implements ResponseTimeStats {

    private static final long NANOSECONDS_PER_SECOND_SQUARED = TimeConversion.NANOSECONDS_PER_SECOND
            * TimeConversion.NANOSECONDS_PER_SECOND;

    private final Object lock = new Object();

    private long total;
    private long totalExclusive;
    private long minValue;
    private long maxValue;
    private double sumOfSquares;

    protected ResponseTimeStatsImpl() {
        super();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return copy();
    }

    @Override
    public void recordResponseTime(long responseTime, TimeUnit timeUnit) {
        long responseTimeInNanos = TimeUnit.NANOSECONDS.convert(responseTime, timeUnit);
        recordResponseTimeInNanos(responseTimeInNanos, responseTimeInNanos);
    }

    @Override
    public void recordResponseTime(long responseTime, long exclusiveTime, TimeUnit timeUnit) {
        long responseTimeInNanos = TimeUnit.NANOSECONDS.convert(responseTime, timeUnit);
        long exclusiveTimeInNanos = TimeUnit.NANOSECONDS.convert(exclusiveTime, timeUnit);
        recordResponseTimeInNanos(responseTimeInNanos, exclusiveTimeInNanos);
    }

    @Override
    public void recordResponseTimeInNanos(long responseTime) {
        recordResponseTimeInNanos(responseTime, responseTime);
    }

    @Override
    public void recordResponseTimeInNanos(long responseTime, long exclusiveTime) {
        double responseTimeAsDouble = responseTime;
        responseTimeAsDouble *= responseTimeAsDouble;
        synchronized (lock) {
            sumOfSquares += responseTimeAsDouble;
            if (getCallCount() > 0) {
                minValue = Math.min(responseTime, minValue);
            } else {
                minValue = responseTime;
            }
            incrementCallCount();
            total += responseTime;
            maxValue = Math.max(responseTime, maxValue);
            totalExclusive += exclusiveTime;
            if (NewRelic.getAgent().getConfig().getValue(AgentConfigImpl.METRIC_DEBUG, AgentConfigImpl.DEFAULT_METRIC_DEBUG)) {
                if (getCallCount() < 0 || total < 0 || totalExclusive < 0 || sumOfSquares < 0) {
                    NewRelic.incrementCounter("Supportability/ResponseTimeStatsImpl/NegativeValue");
                    Agent.LOG.log(Level.INFO, "Invalid count {0}, total {1}, totalExclusive {2}, or sum of squares {3}",
                            getCallCount(), total, totalExclusive, sumOfSquares);
                }
            }
        }
    }

    @Override
    public boolean hasData() {
        boolean hasData;
        synchronized (lock) {
            hasData = getCallCount() > 0 || total > 0 || totalExclusive > 0;
        }
        return hasData;
    }

    @Override
    public void reset() {
        synchronized (lock) {
            setCallCount(0);
            total = totalExclusive = minValue = maxValue = 0;
            sumOfSquares = 0;
        }
    }

    @Override
    public float getTotal() {
        synchronized (lock) {
            return (float) total / TimeConversion.NANOSECONDS_PER_SECOND;
        }
    }

    @Override
    public float getTotalExclusiveTime() {
        synchronized (lock) {
            return (float) totalExclusive / TimeConversion.NANOSECONDS_PER_SECOND;
        }
    }


    @Override
    public float getMaxCallTime() {
        synchronized (lock) {
            return (float) maxValue / TimeConversion.NANOSECONDS_PER_SECOND;
        }
    }

    @Override
    public float getMinCallTime() {
        synchronized (lock) {
            return (float) minValue / TimeConversion.NANOSECONDS_PER_SECOND;
        }
    }

    @Override
    public double getSumOfSquares() {
        synchronized (lock) {
            return sumOfSquares / NANOSECONDS_PER_SECOND_SQUARED;
        }
    }

    private ResponseTimeStatsImpl copy() {
        ResponseTimeStatsImpl newStats = new ResponseTimeStatsImpl();
        synchronized (lock) {
            newStats.setCallCount(this.getCallCount());
            newStats.total = total;
            newStats.totalExclusive = totalExclusive;
            newStats.minValue = minValue;
            newStats.maxValue = maxValue;
            newStats.sumOfSquares = sumOfSquares;
        }
        return newStats;
    }

    @Override
    public final void merge(StatsBase statsObj) {
        if (statsObj instanceof ResponseTimeStatsImpl) {
            ResponseTimeStatsImpl stats = ((ResponseTimeStatsImpl) statsObj).copy();
            synchronized (lock) {
                if (stats.getCallCount() > 0) {
                    if (getCallCount() > 0) {
                        minValue = Math.min(minValue, stats.minValue);
                    } else {
                        minValue = stats.minValue;
                    }
                }
                incrementCallCount(stats.getCallCount());
                total += stats.total;
                totalExclusive += stats.totalExclusive;

                maxValue = Math.max(maxValue, stats.maxValue);
                sumOfSquares += stats.sumOfSquares;
            }
        }
    }

    @Override
    public void recordResponseTime(int count, long totalTime, long minTime, long maxTime, TimeUnit unit) {
        synchronized (lock) {
            long totalTimeInNanos = TimeUnit.NANOSECONDS.convert(totalTime, unit);
            this.setCallCount(count);
            this.total = totalTimeInNanos;
            this.totalExclusive = totalTimeInNanos;
            this.minValue = TimeUnit.NANOSECONDS.convert(minTime, unit);
            this.maxValue = TimeUnit.NANOSECONDS.convert(maxTime, unit);
            double totalTimeInNanosAsDouble = totalTimeInNanos;
            totalTimeInNanosAsDouble *= totalTimeInNanosAsDouble;
            sumOfSquares += totalTimeInNanosAsDouble;
        }
    }

    @Override
    public String toString() {
        return "ResponseTimeStatsImpl [total=" + total + ", totalExclusive=" + totalExclusive + ", minValue="
                + minValue + ", maxValue=" + maxValue + ", sumOfSquares=" + sumOfSquares + "]";

    }

}
