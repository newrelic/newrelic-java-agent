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
        newStats.count = count;
        newStats.total = total;
        newStats.minValue = minValue;
        newStats.maxValue = maxValue;
        newStats.sumOfSquares = sumOfSquares;
        return newStats;
    }

    @Override
    public String toString() {
        return super.toString() + " [total=" + total + ", count=" + count + ", minValue="
                + minValue + ", maxValue=" + maxValue + ", sumOfSquares=" + sumOfSquares + "]";
    }

    @Override
    public void recordDataPoint(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException("Data points must be numbers");
        }
        double sos = sumOfSquares + (value * value);
        if (Double.isInfinite(sos)) {
            throw new IllegalArgumentException("Data value " + value + " caused sum of squares to roll over");
        }
        if (count > 0) {
            minValue = Math.min(value, minValue);
        } else {
            minValue = value;
        }
        count++;
        total += value;
        maxValue = Math.max(value, maxValue);
        sumOfSquares = sos;

        if (NewRelic.getAgent().getConfig().getValue(AgentConfigImpl.METRIC_DEBUG, AgentConfigImpl.DEFAULT_METRIC_DEBUG)) {
            if (count < 0 || total < 0) {
                NewRelic.incrementCounter("Supportability/StatsImpl/NegativeValue");
                Agent.LOG.log(Level.INFO, "Invalid count {0} or total {1}", count, total);

            }
        }

    }

    @Override
    public boolean hasData() {
        return count > 0 || total > 0;
    }

    @Override
    public void reset() {
        count = 0;
        total = minValue = maxValue = 0;
        sumOfSquares = 0;
    }

    @Override
    public float getTotal() {
        return total;
    }

    @Override
    public float getTotalExclusiveTime() {
        return total;
    }

    @Override
    public float getMinCallTime() {
        return minValue;
    }

    @Override
    public float getMaxCallTime() {
        return maxValue;
    }

    @Override
    public double getSumOfSquares() {
        return sumOfSquares;
    }

    @Override
    public void merge(StatsBase statsObj) {
        if (statsObj instanceof StatsImpl) {
            StatsImpl stats = (StatsImpl) statsObj;
            if (stats.count > 0) {
                if (count > 0) {
                    minValue = Math.min(minValue, stats.minValue);
                } else {
                    minValue = stats.minValue;
                }
            }
            count += stats.count;
            total += stats.total;

            maxValue = Math.max(maxValue, stats.maxValue);
            sumOfSquares += stats.sumOfSquares;
        }
    }

}
