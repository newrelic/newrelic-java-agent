/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.circuitbreaker;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.CircuitBreakerConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.agentcontrol.AgentHealth;
import com.newrelic.agent.agentcontrol.HealthDataChangeListener;
import com.newrelic.agent.agentcontrol.HealthDataProducer;
import com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * The CircuitBreakerService is responsible for preventing the agent from crashing an application.<br>
 * <br>
 * When a tracer or transaction is to be created, the breaker is first checked. If it is in a tripped state the tracer
 * will not be created. This prevents the agent from adding overhead that would otherwise cause the application to run
 * out of memory.<br>
 * <br>
 * The decision to trip the breaker is a function of two things: Heap memory usage and time spent garbage collecting.
 * When an application has low free memory and high cpu time garbage collecting, the breaker trips. The breaker will
 * reset at the end of the harvest cycle if it is back in a good state. Every N tracers the breaker is asked if it
 * should trip.<br>
 * <br>
 * Some of the methods on this class run almost immediately after we enter Java code from transformed bytecode. The
 * current thread cannot be assumed to have a TransactionActivity or a Transaction.<br>
 * <br>
 * The default memory and gc thresholds are set in {@link CircuitBreakerConfig}.
 */
public class CircuitBreakerService extends AbstractService implements HarvestListener, AgentConfigListener, HealthDataProducer {
    private static final int TRACER_SAMPLING_RATE = 1000;

    private volatile int tripped = 0;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private volatile GarbageCollectorMXBean oldGenGCBeanCached = null;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<HealthDataChangeListener> healthDataChangeListeners = new CopyOnWriteArrayList<>();


    /**
     * Map of application names to booleans that indicates whether the data to be reported at harvest is incomplete for
     * a given application.
     * */
    private final ConcurrentMap<String, Boolean> missingData;

    /**
     * Only log a CircuitBreaker WARNING level message once.
     */
    private final ThreadLocal<Boolean> logWarning = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };
    private final ThreadLocal<Long> lastTotalGCTimeNS = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return getGCCpuTimeNS();
        }
    };
    private final ThreadLocal<Long> lastTimestampInNanoseconds = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return System.nanoTime();
        }
    };
    private final ThreadLocal<SamplingCounter> tracerSamplerCounter = new ThreadLocal<SamplingCounter>() {
        @Override
        protected SamplingCounter initialValue() {
            return createTracerSamplerCounter();
        }
    };

    public CircuitBreakerService() {
        super(CircuitBreakerService.class.getSimpleName());

        circuitBreakerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getCircuitBreakerConfig();

        if (isEnabled() && null == getOldGenGCBean()) {
            // This shouldn't happen
            Agent.LOG.log(Level.WARNING,
                    "Circuit breaker: Missing required JMX beans. Cannot enable circuit breaker. GC bean: {0}",
                    getOldGenGCBean());
            circuitBreakerConfig.updateEnabled(false);
        }
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
        missingData = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isEnabled() {
        return circuitBreakerConfig.isEnabled();
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        lastTimestampInNanoseconds.set(System.nanoTime());
        lastTotalGCTimeNS.set(getGCCpuTimeNS());

        if (missingData.containsKey(appName) && missingData.get(appName)) {
            recordBreakerOnMetrics(statsEngine, MetricNames.BREAKER_TRIPPED_MEMORY);
        } else {
            recordBreakerOffMetrics(statsEngine);
        }

    }

    private void recordBreakerOnMetrics(StatsEngine statsEngine, String tripCauseMetric) {
        statsEngine.getStats(MetricNames.BREAKER_TRIPPED).incrementCallCount();
        statsEngine.getStats(tripCauseMetric).incrementCallCount();
    }

    private void recordBreakerOffMetrics(StatsEngine statsEngine) {
        statsEngine.recordEmptyStats(MetricNames.BREAKER_TRIPPED);
    }

    @Override
    public void afterHarvest(String appName) {
        if (isTripped() && shouldReset()) {
            reset();
        }
        if (!isTripped()) {
            missingData.put(appName, false);
            // possible race condition: breaker trips, missingData is set to false.
            if (isTripped()) {
                missingData.put(appName, true);
            }
        }
    }

    private boolean shouldTrip() {
        if (!isEnabled()) {
            return false;
        }
        long currentTimeInNanoseconds = System.nanoTime();
        long gcCpuTime = getGCCpuTimeNS() - lastTotalGCTimeNS.get();
        long elapsedTime = currentTimeInNanoseconds - lastTimestampInNanoseconds.get();
        double gcCpuTimePercentage = (gcCpuTime / (double) elapsedTime) * 100;
        if (elapsedTime <= 0) {
            return false;
        }
        double percentageFreeMemory = 100 * ((Runtime.getRuntime().freeMemory() + (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory())) / (double) Runtime.getRuntime().maxMemory());

        lastTimestampInNanoseconds.set(currentTimeInNanoseconds);
        lastTotalGCTimeNS.set(lastTotalGCTimeNS.get() + gcCpuTime);

        int freeMemoryThreshold = circuitBreakerConfig.getMemoryThreshold();
        int gcCPUThreshold = circuitBreakerConfig.getGcCpuThreshold();
        Agent.LOG.log(
                Level.FINEST,
                "Circuit breaker: percentage free memory {0}%  GC CPU time percentage {1}% (freeMemoryThreshold {2}, gcCPUThreshold {3})",
                percentageFreeMemory, gcCpuTimePercentage, freeMemoryThreshold, gcCPUThreshold);
        if (gcCpuTimePercentage >= gcCPUThreshold && percentageFreeMemory <= freeMemoryThreshold) {
            Agent.LOG.log(Level.WARNING, "Circuit breaker tripped at memory {0}%  GC CPU time {1}%", percentageFreeMemory,
                    gcCpuTimePercentage);

            AgentControlIntegrationUtils.reportUnhealthyStatus(healthDataChangeListeners, AgentHealth.Status.GC_CIRCUIT_BREAKER,
                    String.valueOf(percentageFreeMemory), String.valueOf(gcCpuTimePercentage));

            return true;
        }
        return false;
    }

    private boolean shouldReset() {
        return !shouldTrip();
    }

    /**
     * Update the status if the time has come and return the status.
     * 
     * @return status of the circuit breaker.
     */
    public boolean isTripped() {
        if (isEnabled() && tracerSamplerCounter.get().shouldSample() && tripped == 0) {
            checkAndTrip();
        }
        return tripped == 1;
    }

    private void trip() {
        tripped = 1;

        for (String appName : missingData.keySet()) {
            missingData.put(appName, true);
        }

        if (logWarning.get()) {
            logWarning.set(false);
            Agent.LOG.log(
                    Level.WARNING,
                    "Circuit breaker tripped. The agent ceased to create transaction data to perserve heap memory. This may cause incomplete transaction data in the APM UI.");
        }
    }

    /**
     * Reset circuit breaker;
     */
    public void reset() {
        tripped = 0;
        Agent.LOG.log(Level.FINE, "Circuit breaker reset");
        AgentControlIntegrationUtils.reportHealthyStatus(healthDataChangeListeners, AgentHealth.Category.CIRCUIT_BREAKER);
        logWarning.set(true);
    }

    /**
     * Checks memory+gc usage and trips circuit breaker if necessary. This method is public for test purposes and should
     * not be called by client classes in production code. To only check the current status of the circuit breaker, use
     * {@link #isTripped()} instead.
     * 
     * @return true if the circuit breaker was tripped, false otherwise.
     */
    public boolean checkAndTrip() {
        // In production code, this method is called from isTripped() which is called from the various tracer creation
        // code paths. IsTripped() uses a thread-local counter to avoid calling here too often. At low to moderate
        // tracer creation rates, the thread-local counter alone is enough to ensure adequate performance and
        // scalability. But at high rates, a pathological effect emerges: multiple threads begin to pass through the
        // thread-local counter "gate" and execute this method simultaneously. This is pointless, because having this
        // method execute back-to-back is sufficient for circuit breaking, and also very inefficient. It is especially
        // inefficient because isTripped() and shouldTrip() likely contain some synchronization points, so multiple
        // threads likely end up queuing within these methods, forming a scheduling convoy. In order to avoid these
        // behaviors, we wrap the whole checkAndTrip() behavior in a lock. If the lock is held, we just send the
        // calling thread back to its regular business. At low to moderate tracer creation rates, the thread-local
        // counters prevent the serial cost of the lock from becoming a performance issue; at high tracer creation
        // rates, the lock prevents convoys from forming as a result of isTripped() or shouldTrip(). The effect of
        // this lock has been measured using JMH and has been shown to be dramatic: in extremely stressful benchmark
        // with 32 threads busily creating tracers in parallel on a certain 4-core host, the tryLock reduced the tracer
        // overhead from about 3700ns to about 1400ns.

        if (lock.tryLock()) {
            try {
                if (!isTripped() && shouldTrip()) {
                    trip();
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * @return CPU time of old gen GC collection in nanoseconds
     */
    private long getGCCpuTimeNS() {
        return TimeUnit.NANOSECONDS.convert(getOldGenGCBean().getCollectionTime(), TimeUnit.MILLISECONDS);
    }

    private long getGCCount() {
        long gcCpuCount = 0;
        long collectorCount = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            collectorCount = gcBean.getCollectionCount();
            if (collectorCount != -1) {
                gcCpuCount += gcBean.getCollectionCount();
            }
        }
        return gcCpuCount;
    }

    /**
     * Bean with the least amount of gc counts == old gen GC Bean.
     * 
     * @return GarbageCollector bean for the old (tenured) generation pool
     */
    private GarbageCollectorMXBean getOldGenGCBean() {
        if (null != this.oldGenGCBeanCached) {
            return this.oldGenGCBeanCached;
        }
        synchronized (this) {
            if (null != this.oldGenGCBeanCached) {
                return this.oldGenGCBeanCached;
            }

            GarbageCollectorMXBean lowestGCCountBean = null;
            Agent.LOG.log(Level.FINEST, "Circuit breaker: looking for old gen gc bean");

            boolean tie = false;
            long totalGCs = this.getGCCount();

            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                Agent.LOG.log(Level.FINEST, "Circuit breaker: checking {0}", gcBean.getName());
                if (null == lowestGCCountBean || lowestGCCountBean.getCollectionCount() > gcBean.getCollectionCount()) {
                    tie = false;
                    lowestGCCountBean = gcBean;
                    continue;
                }
                if (lowestGCCountBean.getCollectionCount() == gcBean.getCollectionCount()) {
                    tie = true;
                }
            }
            if (getGCCount() == totalGCs && !tie) {
                // gc hasn't happened in the middle of searching and there is a bean with the lowest count
                Agent.LOG.log(Level.FINEST, "Circuit breaker: found and cached oldGenGCBean: {0}",
                        lowestGCCountBean.getName());
                this.oldGenGCBeanCached = lowestGCCountBean;
                return oldGenGCBeanCached;
            } else {
                Agent.LOG.log(Level.FINEST, "Circuit breaker: unable to find oldGenGCBean. Best guess: {0}",
                        lowestGCCountBean.getName());
                return lowestGCCountBean;
            }
        }
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        int newGCCpuThreshold = agentConfig.getCircuitBreakerConfig().getGcCpuThreshold();
        int newMemoryThreshold = agentConfig.getCircuitBreakerConfig().getMemoryThreshold();
        boolean newEnabled = agentConfig.getCircuitBreakerConfig().isEnabled();

        if (newGCCpuThreshold == circuitBreakerConfig.getGcCpuThreshold()
                && newMemoryThreshold == circuitBreakerConfig.getMemoryThreshold()
                && newEnabled == circuitBreakerConfig.isEnabled()) {
            return;
        }

        circuitBreakerConfig.updateEnabled(newEnabled);
        circuitBreakerConfig.updateThresholds(newGCCpuThreshold, newMemoryThreshold);
        Agent.LOG.log(Level.INFO,
                "Circuit breaker: updated configuration - enabled {0} GC CPU Threshold {1}% Memory Threshold {2}%.",
                circuitBreakerConfig.isEnabled(), circuitBreakerConfig.getGcCpuThreshold(),
                circuitBreakerConfig.getMemoryThreshold());
    }

    public void addRPMService(RPMService rpmService) {
        missingData.put(rpmService.getApplicationName(), isTripped());
    }

    public void removeRPMService(RPMService rpmService) {
        missingData.remove(rpmService.getApplicationName());
    }

    /**
     * Only use for testing.
     */
    public void setPreviousChecksForTesting(long newGCTimeNS, long newCpuTimeNS) {
        lastTotalGCTimeNS.set(newGCTimeNS);
        lastTimestampInNanoseconds.set(newCpuTimeNS);
    }

    public static SamplingCounter createTracerSamplerCounter() {
        return new SamplingCounter(TRACER_SAMPLING_RATE);
    }

    @Override
    public void registerHealthDataChangeListener(HealthDataChangeListener listener) {
        healthDataChangeListeners.add(listener);
    }
}
