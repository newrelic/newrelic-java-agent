/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.circuitbreaker.CircuitBreakerService;

import java.util.Map;

/**
 * See {@link CircuitBreakerService}
 */
public class CircuitBreakerConfig extends BaseConfig {
    public static final String ENABLED = "enabled";
    public static final Boolean DEFAULT_ENABLED = Boolean.TRUE;

    public static final String MEMORY_THRESHOLD = "memory_threshold";
    public static final int DEFAULT_MEMORY_THRESHOLD = 20;
    public static final String GC_CPU_THRESHOLD = "gc_cpu_threshold";
    public static final int DEFAULT_GC_CPU_THRESHOLD = 10;

    public static final String PROPERTY_NAME = "circuitbreaker";
    public static final String PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";

    private boolean isEnabled;
    private int memoryThreshold;
    private int gcCpuThreshold;

    public CircuitBreakerConfig(Map<String, Object> pProps) {
        super(pProps, PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        memoryThreshold = getProperty(MEMORY_THRESHOLD, DEFAULT_MEMORY_THRESHOLD);
        gcCpuThreshold = getProperty(GC_CPU_THRESHOLD, DEFAULT_GC_CPU_THRESHOLD);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public int getGcCpuThreshold() {
        return gcCpuThreshold;
    }

    public boolean updateThresholds(int newGCCpuThreshold, int newMemoryThreshold) {
        if (newGCCpuThreshold >= 0 && newMemoryThreshold >= 0) {
            gcCpuThreshold = newGCCpuThreshold;
            memoryThreshold = newMemoryThreshold;
            return true;
        }
        return false;
    }

    public boolean updateEnabled(boolean newEnabled) {
        if (isEnabled != newEnabled) {
            isEnabled = newEnabled;
            return true;
        }
        return false;
    }
}
