/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

/**
 * This class is not thread-safe.
 */
public class StatsImpl extends AbstractStats implements Stats {

    private final Object lock = new Object();

    private float total;
    private float minValue;
    private float maxValue;
    private double sumOfSquares;

    protected StatsImpl() {
        super();
    }

    public StatsImpl(int count, float total, float minValue, float maxValue, double sumOfSquares) {
        super(count);
        this.total = total;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sumOfSquares = sumOfSquares;

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        StatsImpl newStats = new StatsImpl();
        synchronized (lock) {
            newStats.setCallCount(this.getCallCount());
            newStats.total = total;
            newStats.minValue = minValue;
            newStats.maxValue = maxValue;
            newStats.sumOfSquares = sumOfSquares;
        }
        return newStats;
    }

    @Override
    public String toString() {
        return super.toString() + " [total=" + total + ", count=" + this.getCallCount() + ", minValue="
                + minValue + ", maxValue=" + maxValue + ", sumOfSquares=" + sumOfSquares + "]";
    }

    @Override
    public void recordDataPoint(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException("Data points must be numbers");
        }
        synchronized (lock) {
            double sos = sumOfSquares + (value * value);
            if (sos < sumOfSquares) {
                throw new IllegalArgumentException("Data value " + value + " caused sum of squares to roll over");
            }
            if (this.getCallCount() > 0) {
                minValue = Math.min(value, minValue);
            } else {
                minValue = value;
            }
            incrementCallCount();
            total += value;
            maxValue = Math.max(value, maxValue);
            sumOfSquares = sos;

            if (NewRelic.getAgent().getConfig().getValue(AgentConfigImpl.METRIC_DEBUG, AgentConfigImpl.DEFAULT_METRIC_DEBUG)) {
                if (getCallCount() < 0 || total < 0) {
                    NewRelic.incrementCounter("Supportability/StatsImpl/NegativeValue");
                    Agent.LOG.log(Level.INFO, "Invalid count {0} or total {1}", getCallCount(), total);

                }
            }
        }
    }

    @Override
    public boolean hasData() {
        synchronized (lock) {
            return getCallCount() > 0 || total > 0;
        }
    }

    @Override
    public void reset() {
        synchronized (lock) {
            setCallCount(0);
            total = minValue = maxValue = 0;
            sumOfSquares = 0;
        }
    }

    @Override
    public float getTotal() {
        synchronized (lock) {
            return total;
        }
    }

    @Override
    public float getTotalExclusiveTime() {
        synchronized (lock) {
            return total;
        }
    }

    @Override
    public float getMinCallTime() {
        synchronized (lock) {
            return minValue;
        }
    }

    @Override
    public float getMaxCallTime() {
        synchronized (lock) {
            return maxValue;
        }
    }

    @Override
    public double getSumOfSquares() {
        synchronized (lock) {
            return sumOfSquares;
        }
    }

    @Override
    public void merge(StatsBase statsObj) {
        if (statsObj instanceof StatsImpl) {
            StatsImpl stats = (StatsImpl) statsObj;
            synchronized (lock) {
                if (stats.getCallCount() > 0) {
                    if (getCallCount() > 0) {
                        minValue = Math.min(minValue, stats.minValue);
                    } else {
                        minValue = stats.minValue;
                    }
                }
                setCallCount(stats.getCallCount());
                total += stats.total;

                maxValue = Math.max(maxValue, stats.maxValue);
                sumOfSquares += stats.sumOfSquares;
            }
        }
    }
}
