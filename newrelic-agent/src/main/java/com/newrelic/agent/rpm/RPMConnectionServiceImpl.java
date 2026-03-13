/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.rpm;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * This class is responsible for running the New Relic connection tasks.
 *
 * This class is thread-safe.
 */
public class RPMConnectionServiceImpl extends AbstractService implements RPMConnectionService {

    public static final String RPM_CONNECTION_THREAD_NAME = "New Relic RPM Connection Service";
    // delay for first app server port availability check
    public static final long INITIAL_APP_SERVER_PORT_DELAY = 5L;
    // delay for subsequent app server port availability checks
    public static final long SUBSEQUENT_APP_SERVER_PORT_DELAY = 5L;
    // max time to wait for app server port
    public static final long APP_SERVER_PORT_TIMEOUT = 120L;
    // interval between connection attempts
    public static final long MIN_CONNECT_ATTEMPT_INTERVAL = 5L;
    private static final int MAX_QUEUED_CONNECT_TASKS = 5000;
    private final ScheduledExecutorService scheduledExecutor;

    public RPMConnectionServiceImpl() {
        super(RPMConnectionService.class.getSimpleName());
        ThreadFactory threadFactory = new DefaultThreadFactory(RPM_CONNECTION_THREAD_NAME, true);
        scheduledExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
        scheduledExecutor.shutdown();
    }

    /**
     * Start a connection task for the service.
     */
    @Override
    public void connect(IRPMService rpmService) {
        RPMConnectionTask connectionTask = new RPMConnectionTask(rpmService);
        connectionTask.start();
    }

    /**
     * Start an immediate connection task for the RPM service.
     */
    @Override
    public void connectImmediate(IRPMService rpmService) {
        RPMConnectionTask connectionTask = new RPMConnectionTask(rpmService);
        connectionTask.startImmediate();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Tests can override.
     */
    public long getInitialAppServerPortDelay() {
        return INITIAL_APP_SERVER_PORT_DELAY;
    }

    /**
     * Tests can override.
     */
    public long getAppServerPortTimeout() {
        return APP_SERVER_PORT_TIMEOUT;
    }

    /**
     * Checks whether tasks have begun piling up in the scheduled executor's work queue.
     * Used to prevent repeated reconnect attempts in the event of a LicenseException.
     */
    public boolean shouldPreventNewConnectionTask(){
        return ((ScheduledThreadPoolExecutor) scheduledExecutor).getQueue().size() > MAX_QUEUED_CONNECT_TASKS;
    }

    /**
     * The RPM connection task is responsible for connecting to New Relic.
     */
    private final class RPMConnectionTask implements Runnable {

        private final IRPMService rpmService;
        private final AtomicReference<ScheduledFuture<?>> appServerPortTask = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> appServerPortTimeoutTask = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> connectTask = new AtomicReference<>();
        private final AtomicBoolean connectTaskStarted = new AtomicBoolean();

        private final int[] BACKOFF_INTERVAL_IN_SEC = new int[] { 0, 15, 15, 30, 60, 120, 300 };
        private final int MAX_BACKOFF_DELAY = 300;
        private final AtomicLong lastConnectionAttempt = new AtomicLong(0);
        private final AtomicInteger backoffIndex = new AtomicInteger(0);

        private RPMConnectionTask(IRPMService rpmService) {
            this.rpmService = rpmService;
        }

        @Override
        public void run() {
        }

        /**
         * Runs until a connection to New Relic is established.
         *
         * If the RPM service is not for the main application, the 'sync_startup' configuration setting is true, or serverless mode is enabled,
         * then don't wait for the application server port.
         */
        private void start() {
            if (!rpmService.isMainApp()) {
                startImmediate();
            } else if (isSyncStartup() || isLaspEnabled() || isHighSecurityEnabled() || isServerlessModeEnabled()) {
                getLogger().log(Level.FINER, "Not waiting for application server port");
                startSync();
            } else {
                getLogger().log(Level.FINER, "Waiting for application server port");
                appServerPortTask.set(scheduleAppServerPortTask());
                appServerPortTimeoutTask.set(scheduleAppServerPortTimeoutTask());
            }
        }

        /**
         * Make one synchronous attempt to connect. If it fails, start an asynchronous connection task.
         */
        private void startSync() {
            if (isConnected() || attemptConnection()) {
                return;
            }
            startImmediate();
        }

        /**
         * Runs until a connection to New Relic is established without waiting for the application server port.
         */
        private void startImmediate() {
            connectTask.set(scheduleConnectTask());
        }

        /**
         * Cancel all tasks.
         */
        private void stop() {
            getLogger().log(Level.FINER, "Stopping New Relic connection task for {0}", rpmService.getApplicationName());
            ScheduledFuture<?> handle = appServerPortTask.get();
            if (handle != null) {
                handle.cancel(false);
            }
            handle = connectTask.get();
            if (handle != null) {
                handle.cancel(false);
            }
            handle = appServerPortTimeoutTask.get();
            if (handle != null) {
                handle.cancel(false);
            }

            lastConnectionAttempt.set(0);
            backoffIndex.set(0);
        }

        /**
         * A task to check the availability of the app server port and start the connect task once it's available, if
         * not already started or connected.
         *
         * Check every 5 seconds after initial delay of 30 seconds.
         */
        private ScheduledFuture<?> scheduleAppServerPortTask() {
            return scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(new Runnable() {
                @Override
                public void run() {
                    if (isConnected()) {
                        stop();
                        return;
                    }
                    if (hasAppServerPort() && !connectTaskStarted()) {
                        stop();
                        getLogger().log(Level.FINER, "Discovered application server port");
                        connectTask.set(scheduleConnectTask());
                    }
                }
            }), getInitialAppServerPortDelay(), SUBSEQUENT_APP_SERVER_PORT_DELAY, TimeUnit.SECONDS);
        }

        /**
         * A task to start the connect task after 120 seconds whether the app server port is available or not, if not
         * already started or connected.
         */
        private ScheduledFuture<?> scheduleAppServerPortTimeoutTask() {
            return scheduledExecutor.schedule(SafeWrappers.safeRunnable(new Runnable() {
                @Override
                public void run() {
                    if (!connectTaskStarted()) {
                        stop();
                        if (!isConnected()) {
                            if (!hasAppServerPort()) {
                                getLogger().log(Level.FINER, "Gave up waiting for application server port");
                            }
                            connectTask.set(scheduleConnectTask());
                        }
                    }
                }
            }), getAppServerPortTimeout(), TimeUnit.SECONDS);
        }

        /**
         * A task for connecting to the RPM service. Attempt a connection using the backoff sequence of
         * [0, 15, 15, 30, 60, 120, 300] seconds, with no initial delay. Delay continues indefinitely at 300 seconds.
         *
         * If the RPM service is not for the main application, and the RPM service for the main application is not
         * connected, then don't attempt to connect because the RPM service needs to send the agent run id for the main
         * application to RPM.
         */
        private ScheduledFuture<?> scheduleConnectTask() {
            return scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(new Runnable() {
                @Override
                public void run() {
                    if (shouldAttemptConnection() && attemptConnection()) {
                        stop();
                    }
                }
            }), 0L, MIN_CONNECT_ATTEMPT_INTERVAL, TimeUnit.SECONDS);
        }

        /**
         * Is the main application connected?
         */
        private boolean isMainAppConnected() {
            return ServiceFactory.getRPMService().isConnected();
        }

        /**
         * Is RPM connected?
         *
         * @return <tt>true</tt> if the RPM service is already connected, <tt>false</tt> otherwise
         */
        private boolean isConnected() {
            return rpmService.isConnected();
        }

        /**
         * Mark the connect task as started. This method is used to synchronize the start of the connect task.
         *
         * @return <tt>true</tt> if the connect task has already started, <tt>false</tt> otherwise
         */
        private boolean connectTaskStarted() {
            return connectTaskStarted.getAndSet(true);
        }

        /**
         * Has the app server port been discovered yet?
         *
         * @return <tt>true</tt> if the app server port is known, <tt>false</tt> otherwise
         */
        private boolean hasAppServerPort() {
            return getEnvironment().getAgentIdentity().getServerPort() != null;
        }

        /**
         * Returns <tt>true</tt> if at least one synchronous connection attempt should be made.
         */
        private boolean isSyncStartup() {
            ConfigService configService = ServiceFactory.getConfigService();
            AgentConfig config = configService.getAgentConfig(rpmService.getApplicationName());
            return config.isSyncStartup();
        }

        private boolean isServerlessModeEnabled() {
            ConfigService configService = ServiceFactory.getConfigService();
            AgentConfig config = configService.getAgentConfig(rpmService.getApplicationName());
            return config.getServerlessConfig().isEnabled();
        }

        private boolean isLaspEnabled() {
            return ServiceFactory.getConfigService().getDefaultAgentConfig().laspEnabled();
        }

        private boolean isHighSecurityEnabled() {
            return ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity();
        }

        private boolean shouldAttemptConnection() {
            if (!shouldAttemptBackoff()) {
                return false;
            }
            if (rpmService.isMainApp() || isMainAppConnected()) {
                return !isConnected();
            }
            return false;
        }

        /**
         * Determines if the agent should attempt to connect to RPM based on the backoff algorithm of [0, 15, 15, 30, 60, 120, 300] seconds
         *
         * @return true if the agent should retry to connect, false otherwise
         */
        private boolean shouldAttemptBackoff() {
            lastConnectionAttempt.compareAndSet(0, System.currentTimeMillis());
            long timeSinceLastConnectionAttemptInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastConnectionAttempt.get());

            if (timeSinceLastConnectionAttemptInSeconds >= BACKOFF_INTERVAL_IN_SEC[backoffIndex.get()]) {
                ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_AGENT_CONNECT_BACKOFF_ATTEMPTS, 1), MetricNames.SUPPORTABILITY_AGENT_CONNECT_BACKOFF_ATTEMPTS);

                if (timeSinceLastConnectionAttemptInSeconds < MAX_BACKOFF_DELAY) {
                    backoffIndex.incrementAndGet();
                }

                lastConnectionAttempt.set(System.currentTimeMillis());
                return true;
            }

            return false;
        }

        /**
         * Try to connect to RPM.
         *
         * @return <tt>true</tt> if the connection attempt is successful, <tt>false</tt> otherwise
         */
        private boolean attemptConnection() {
            try {
                rpmService.launch();
                return true;
            } catch (Throwable e) {
                // non-ForceDisconnectException HttpErrors are caught here
                getLogger().log(Level.INFO, "Failed to connect to {0} for {1}: {2}", rpmService.getHostString(), rpmService.getApplicationName(), e.toString());
                getLogger().log(Level.FINEST, e, e.toString());
            }
            return false;
        }

        private Environment getEnvironment() {
            return ServiceFactory.getEnvironmentService().getEnvironment();
        }
    }

}
