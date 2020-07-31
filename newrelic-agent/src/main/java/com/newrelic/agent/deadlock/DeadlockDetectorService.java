/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

/**
 * A service for detecting deadlocked threads.
 * 
 * This class is thread-safe.
 */
public class DeadlockDetectorService extends AbstractService {

    private static final String DEADLOCK_DETECTOR_THREAD_NAME = "New Relic Deadlock Detector";
    private static final long INITIAL_DELAY_IN_SECONDS = 300L;
    private static final long SUBSEQUENT_DELAY_IN_SECONDS = 300L;

    private final ErrorCollectorConfig errorCollectorConfig;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final boolean isEnabled;
    private final ScheduledExecutorService scheduledExecutor;
    private volatile ScheduledFuture<?> deadlockTask;

    public DeadlockDetectorService() {
        super(DeadlockDetectorService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        isEnabled = config.getValue("deadlock_detector.enabled", true);
        errorCollectorConfig = config.getErrorCollectorConfig();

        ThreadFactory threadFactory = (isEnabled) ? new DefaultThreadFactory(DEADLOCK_DETECTOR_THREAD_NAME, true)
                : null;
        scheduledExecutor = (isEnabled) ? Executors.newSingleThreadScheduledExecutor(threadFactory) : null;
    }

    @Override
    protected void doStart() {
        if (!isEnabled) {
            logger.log(Level.FINE, "The Deadlock detector is disabled.");
            return;
        }
        if (!threadMXBean.isObjectMonitorUsageSupported()) {
            logger.log(Level.FINE, "JVM does not support monitoring of object monitor usage. The Deadlock detector is disabled.");
            return;
        }

        if (!threadMXBean.isSynchronizerUsageSupported()) {
            logger.log(Level.FINE, "JVM does not support monitoring of ownable synchronizer usage. The Deadlock detector is disabled.");
            return;
        }

        final DeadLockDetector deadlockDetector = getDeadlockDetector();

        try {
            deadlockDetector.detectDeadlockedThreads();
        } catch (Throwable t) {
            logger.log(Level.FINE, t, "Failed to detect deadlocked threads: {0}.  The Deadlock detector is disabled.",
                    t.toString());
            logger.log(Level.FINEST, t, t.toString());
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    deadlockDetector.detectDeadlockedThreads();
                } catch (Throwable t) {
                    String msg = MessageFormat.format("Failed to detect deadlocked threads: {0}", t.toString());
                    if (getLogger().isLoggable(Level.FINER)) {
                        getLogger().log(Level.WARNING, msg, t);
                    } else {
                        getLogger().warning(msg);
                    }
                }
            }
        };

        deadlockTask = scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(runnable),
                INITIAL_DELAY_IN_SECONDS, SUBSEQUENT_DELAY_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void doStop() {
        if (!isEnabled) {
            return;
        }
        if (deadlockTask != null) {
            deadlockTask.cancel(false);
        }
        scheduledExecutor.shutdown();
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Returns a deadlock detector.
     */
    private DeadLockDetector getDeadlockDetector() {
        return new DeadLockDetector(errorCollectorConfig);
    }
}
