/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.TimeConversion;

/**
 * Samples CPU utilization using JMX. Java 1.5 required.
 * 
 * This class is not thread-safe.
 */
public abstract class AbstractCPUSampler {
    private double lastCPUTimeSeconds;
    private long lastTimestampNanos;
    private final int processorCount;

    protected AbstractCPUSampler() {
        processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        Agent.LOG.finer(processorCount + " processor(s)");
    }

    /**
     * Returns the process cpu time in seconds.
     * 
     */
    protected abstract double getProcessCpuTime();

    protected void recordCPU(StatsEngine statsEngine) {

        double currentProcessTime = getProcessCpuTime();
        double dCPU = currentProcessTime - lastCPUTimeSeconds;
        lastCPUTimeSeconds = currentProcessTime;

        long now = System.nanoTime();
        long elapsedNanos = now - lastTimestampNanos;
        lastTimestampNanos = now;

        double elapsedTime = TimeConversion.convertNanosToSeconds(elapsedNanos);
        double utilization = dCPU / (elapsedTime * processorCount);

        boolean shouldLog = Agent.LOG.isLoggable(Level.FINER);
        if (shouldLog) {
            String msg = MessageFormat.format("Recorded CPU time: {0} ({1}) {2}", dCPU, utilization,
                    getClass().getName());
            Agent.LOG.finer(msg);
        }
        if (lastCPUTimeSeconds > 0 && dCPU >= 0) {
            if (Double.isNaN(dCPU) || Double.isInfinite(dCPU)) {
                if (shouldLog) {
                    String msg = MessageFormat.format("Infinite or non-number CPU time: {0} (current) - {1} (last)",
                            currentProcessTime, lastCPUTimeSeconds);
                    Agent.LOG.finer(msg);
                }
            } else {
                statsEngine.getStats(MetricNames.CPU).recordDataPoint((float) dCPU);
            }
            if (Double.isNaN(utilization) || Double.isInfinite(utilization)) {
                if (shouldLog) {
                    String msg = MessageFormat.format("Infinite or non-number CPU utilization: {0} ({1})", utilization,
                            dCPU);
                    Agent.LOG.finer(msg);
                }
            } else {
                statsEngine.getStats(MetricNames.CPU_UTILIZATION).recordDataPoint((float) utilization);
            }
        } else {
            if (shouldLog) {
                String msg = MessageFormat.format("Bad CPU time: {0} (current) - {1} (last)", currentProcessTime,
                        lastCPUTimeSeconds);
                Agent.LOG.finer(msg);
            }
        }
    }
}
