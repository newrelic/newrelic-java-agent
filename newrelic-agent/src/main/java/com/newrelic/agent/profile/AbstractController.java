/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.lang.management.ManagementFactory;

import com.newrelic.agent.stats.StatsEngine;

/**
 * Abstract class to calculate the sample period of a profiling task.
 * 
 * This class is not thread-safe: use only in the Harvest Service thread, except that the run() method is called in the
 * profiling task thread.
 */
public abstract class AbstractController implements ProfilingTaskController {

    static int MAX_SAMPLE_PERIOD_IN_MILLIS = 6400;
    static int MIN_SAMPLE_PERIOD_IN_MILLIS = 100;
    static float TARGET_UTILIZATION = 0.02f; // target utilization (2% of capacity)

    private final ProfilingTask delegate;
    private final int processorCount;
    private int samplePeriodInMillis = -1;

    public AbstractController(ProfilingTask delegate) {
        this.delegate = delegate;
        processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

    protected int getProcessorCount() {
        return processorCount;
    }

    abstract int doCalculateSamplePeriodInMillis();

    @Override
    public int getSamplePeriodInMillis() {
        if (samplePeriodInMillis == -1) {
            return MIN_SAMPLE_PERIOD_IN_MILLIS;
        }
        return samplePeriodInMillis;
    }

    private void calculateSamplePeriodInMillis() {
        if (samplePeriodInMillis == -1) {
            return;
        }
        int nSamplePeriodInMillis = doCalculateSamplePeriodInMillis();
        if (nSamplePeriodInMillis > samplePeriodInMillis) {
            // double the sample period
            nSamplePeriodInMillis = samplePeriodInMillis * 2;
        } else if (nSamplePeriodInMillis <= samplePeriodInMillis / 4) {
            // halve the sample period
            nSamplePeriodInMillis = samplePeriodInMillis / 2;
        } else {
            nSamplePeriodInMillis = samplePeriodInMillis;
        }
        // constrain the sample period to <= max sample period and >= min sample period
        nSamplePeriodInMillis = Math.min(AbstractController.MAX_SAMPLE_PERIOD_IN_MILLIS, Math.max(
                nSamplePeriodInMillis, MIN_SAMPLE_PERIOD_IN_MILLIS));
        samplePeriodInMillis = nSamplePeriodInMillis;
    }

    @Override
    public void run() {
        delegate.run();
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        delegate.beforeHarvest(appName, statsEngine);
    }

    @Override
    public void afterHarvest(String appName) {
        calculateSamplePeriodInMillis();
        delegate.afterHarvest(appName);
    }

    @Override
    public void addProfile(ProfilerParameters parameters) {
        if (samplePeriodInMillis == -1) {
            samplePeriodInMillis = parameters.getSamplePeriodInMillis().intValue();
        }
        delegate.addProfile(parameters);
    }

    @Override
    public void removeProfile(ProfilerParameters parameters) {
        delegate.removeProfile(parameters);
    }

    /**
     * For testing.
     */
    ProfilingTask getDelegate() {
        return delegate;
    }

}
