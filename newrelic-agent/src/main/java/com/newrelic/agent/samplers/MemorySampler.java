/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.stats.StatsEngine;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

public class MemorySampler implements MetricSampler {

    public static final float BYTES_PER_MB = 1048576f;

    private final MemoryMXBean memoryMXBean;

    public MemorySampler() {
        memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    void start() {
    }

    @Override
    public void sample(StatsEngine statsEngine) {
        sampleMemory(statsEngine);
        sampleMemoryPools(statsEngine);
    }

    private void sampleMemory(StatsEngine statsEngine) {
        try {
            HeapAndNonHeapUsage heapUsage = new HeapAndNonHeapUsage(memoryMXBean);
            heapUsage.recordStats(statsEngine);
        } catch (Exception e) {
            String msg = MessageFormat.format("An error occurred gathering memory metrics: {0}", e);
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.WARNING, msg, e);
            } else {
                Agent.LOG.warning(msg);
            }
        }
    }

    private void sampleMemoryPools(StatsEngine statsEngine) {
        try {
            List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
                PoolUsage poolUsage = new PoolUsage(memoryPoolMXBean);
                poolUsage.recordStats(statsEngine);
            }
            List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
            for (BufferPoolMXBean bufferPoolMXBean : bufferPoolMXBeans) {
                PoolUsage poolUsage = new PoolUsage(bufferPoolMXBean);
                poolUsage.recordStats(statsEngine);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("An error occurred gathering memory pool metrics: {0}", e);
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.WARNING, msg, e);
            } else {
                Agent.LOG.warning(msg);
            }
        }
    }

    /**
     * A class to record stats for heap and non-heap memory usage in megabytes from a {@link MemoryMXBean}.
     */
    private static final class HeapAndNonHeapUsage {

        private final long heapUsed;
        private final long heapCommitted;
        private final long heapMax;

        private final long nonHeapUsed;
        private final long nonHeapCommitted;
        private final long nonHeapMax;

        private HeapAndNonHeapUsage(MemoryMXBean memoryMXBean) {
            MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
            nonHeapCommitted = nonHeapMemoryUsage.getCommitted();
            nonHeapUsed = nonHeapMemoryUsage.getUsed();
            nonHeapMax = nonHeapMemoryUsage.getMax() == -1 ? 0 : nonHeapMemoryUsage.getMax();

            MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
            heapUsed = heapMemoryUsage.getUsed();
            heapCommitted = heapMemoryUsage.getCommitted();
            heapMax = heapMemoryUsage.getMax() == -1 ? 0 : heapMemoryUsage.getMax();
        }

        public void recordStats(StatsEngine statsEngine) {
            statsEngine.getStats(MetricNames.MEMORY).recordDataPoint(getCommitted());
            statsEngine.getStats(MetricNames.MEMORY_USED).recordDataPoint(getUsed());
            statsEngine.getStats(MetricNames.HEAP_USED).recordDataPoint(getHeapUsed());
            statsEngine.getStats(MetricNames.HEAP_COMMITTED).recordDataPoint(getHeapCommitted());
            statsEngine.getStats(MetricNames.HEAP_MAX).recordDataPoint(getHeapMax());
            statsEngine.getStats(MetricNames.HEAP_UTILIZATION).recordDataPoint(getHeapUtilization());
            statsEngine.getStats(MetricNames.NON_HEAP_USED).recordDataPoint(getNonHeapUsed());
            statsEngine.getStats(MetricNames.NON_HEAP_COMMITTED).recordDataPoint(getNonHeapCommitted());
            statsEngine.getStats(MetricNames.NON_HEAP_MAX).recordDataPoint(getNonHeapMax());
        }

        private float getCommitted() {
            return (nonHeapCommitted + heapCommitted) / BYTES_PER_MB;
        }

        private float getUsed() {
            return (nonHeapUsed + heapUsed) / BYTES_PER_MB;
        }

        private float getHeapUsed() {
            return heapUsed / BYTES_PER_MB;
        }

        private float getHeapUtilization() {
            return heapMax == 0 ? 0 : (float) heapUsed / heapMax;
        }

        private float getHeapCommitted() {
            return heapCommitted / BYTES_PER_MB;
        }

        private float getHeapMax() {
            return heapMax / BYTES_PER_MB;
        }

        private float getNonHeapUsed() {
            return nonHeapUsed / BYTES_PER_MB;
        }

        private float getNonHeapCommitted() {
            return nonHeapCommitted / BYTES_PER_MB;
        }

        private float getNonHeapMax() {
            return nonHeapMax / BYTES_PER_MB;
        }
    }

    /**
     * A class to record stats for memory pool usage in megabytes from a {@link MemoryPoolMXBean}.
     */
    private static final class PoolUsage {

        private final String name;
        private final String type;
        private final long used;
        private final long committed;
        private final long max;

        private PoolUsage(MemoryPoolMXBean memoryPoolMXBean) {
            name = memoryPoolMXBean.getName();
            type = memoryPoolMXBean.getType() == MemoryType.HEAP ? "Heap" : "Non-Heap";
            MemoryUsage memoryUsage = memoryPoolMXBean.getUsage();
            used = memoryUsage.getUsed();
            committed = memoryUsage.getCommitted();
            max = memoryUsage.getMax() == -1 ? 0 : memoryUsage.getMax();
        }

        private PoolUsage(BufferPoolMXBean bufferPoolMXBean) {
            name = bufferPoolMXBean.getName();
            type = "Non-Heap";
            used = bufferPoolMXBean.getMemoryUsed();
            committed = bufferPoolMXBean.getTotalCapacity();
            max = 0;
        }

        public void recordStats(StatsEngine statsEngine) {
            String metricName = MessageFormat.format(MetricNames.MEMORY_POOL_USED_MASK, type, name);
            statsEngine.getStats(metricName).recordDataPoint(getUsed());
            metricName = MessageFormat.format(MetricNames.MEMORY_POOL_COMMITTED_MASK, type, name);
            statsEngine.getStats(metricName).recordDataPoint(getCommitted());
            metricName = MessageFormat.format(MetricNames.MEMORY_POOL_MAX_MASK, type, name);
            statsEngine.getStats(metricName).recordDataPoint(getMax());
        }

        private float getUsed() {
            return used / BYTES_PER_MB;
        }

        private float getCommitted() {
            return committed / BYTES_PER_MB;
        }

        private float getMax() {
            return max / BYTES_PER_MB;
        }
    }
}
