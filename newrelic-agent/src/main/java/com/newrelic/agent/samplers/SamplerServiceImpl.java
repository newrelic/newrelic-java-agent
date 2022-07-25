/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import com.google.common.collect.Sets;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.MergeStatsEngine;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A service for running metric samplers.
 * 
 * This class is thread-safe.
 */
public class SamplerServiceImpl extends AbstractService implements SamplerService {

    private static final String SAMPLER_THREAD_NAME = "New Relic Sampler Service";
    private static final int INITIAL_DELAY_IN_MILLISECONDS = 60000;
    private static final long DELAY_IN_MILLISECONDS = 5000L;

    private final ScheduledExecutorService scheduledExecutor;
    private final Set<ScheduledFuture<?>> tasks = Sets.newSetFromMap(new ConcurrentHashMap<ScheduledFuture<?>, Boolean>());
    private final StatsEngine statsEngine = new StatsEngineImpl();
    private final CoreService coreService;
    private final String defaultAppName;
    private final boolean isAutoAppNamingEnabled;
    private final long memorySampleDelayInMillis;

    public SamplerServiceImpl() {
        super(SamplerService.class.getSimpleName());
        ThreadFactory threadFactory = new DefaultThreadFactory(SAMPLER_THREAD_NAME, true);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        coreService = ServiceFactory.getCoreService();
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        isAutoAppNamingEnabled = config.isAutoAppNamingEnabled();
        defaultAppName = config.getApplicationName();
        memorySampleDelayInMillis = config.getValue("sampler_service.memory_sample_delay_in_millis",
                INITIAL_DELAY_IN_MILLISECONDS);
    }

    @Override
    protected void doStart() {
        MemorySampler memorySampler = new MemorySampler();
        memorySampler.start();
        addMetricSampler(memorySampler, memorySampleDelayInMillis, DELAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);

        ThreadSampler threadSampler = new ThreadSampler();
        addMetricSampler(threadSampler, INITIAL_DELAY_IN_MILLISECONDS, DELAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doStop() {
        synchronized (tasks) {
            for (ScheduledFuture<?> task : tasks) {
                task.cancel(false);
            }
            tasks.clear();
        }
        scheduledExecutor.shutdown();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private void addMetricSampler(final MetricSampler sampler, final long initialDelay, final long delay,
            final TimeUnit unit) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    runSampler(sampler);
                } catch (Throwable t) {
                    String msg = MessageFormat.format("Unable to sample {0}: {1}", getClass().getName(), t);
                    if (getLogger().isLoggable(Level.FINER)) {
                        getLogger().log(Level.WARNING, msg, t);
                    } else {
                        getLogger().warning(msg);
                    }
                } finally {
                    statsEngine.clear();
                }
            }
        };
        addSampler(runnable, initialDelay, delay, unit);
    }

    private void runSampler(MetricSampler sampler) {
        if (!coreService.isEnabled()) {
            return;
        }
        sampler.sample(statsEngine);
        if (!isAutoAppNamingEnabled) {
            mergeStatsEngine(defaultAppName);
            return;
        }
        List<IRPMService> rpmServices = ServiceFactory.getRPMServiceManager().getRPMServices();
        for (IRPMService rpmService : rpmServices) {
            String appName = rpmService.getApplicationName();
            mergeStatsEngine(appName);
        }
    }

    private void mergeStatsEngine(String appName) {
        StatsService statsService = ServiceFactory.getStatsService();
        StatsWork work = new MergeStatsEngine(appName, statsEngine);
        statsService.doStatsWork(work, statsService.getName());
    }

    @Override
    public Closeable addSampler(Runnable sampler, long period, TimeUnit timeUnit) {
        return addSampler(sampler, period, period, timeUnit);
    }

    @Override
    public Closeable addSampler(Runnable sampler, long initialDelay, long period, TimeUnit timeUnit) {
        if (scheduledExecutor.isShutdown()) {
            return null;
        }
        final ScheduledFuture<?> task = scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(sampler),
                initialDelay, period, timeUnit);
        tasks.add(task);
        return new Closeable() {
            @Override
            public void close() throws IOException {
                tasks.remove(task);
                task.cancel(false);
            }
        };
    }
}
