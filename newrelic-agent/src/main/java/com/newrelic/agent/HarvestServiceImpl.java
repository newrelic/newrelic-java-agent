/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import static com.newrelic.agent.config.SpanEventsConfig.SERVER_SPAN_HARVEST_CONFIG;
import static com.newrelic.agent.config.SpanEventsConfig.SERVER_SPAN_HARVEST_LIMIT;
import static com.newrelic.agent.transport.CollectorMethods.SPAN_EVENT_DATA;

/**
 * This class is responsible for running the harvest tasks. There is one harvest task per RPM service. A harvest task
 * reports metric data to the server.
 */
public class HarvestServiceImpl extends AbstractService implements HarvestService {

    public static final String HARVEST_THREAD_NAME = "New Relic Harvest Service";
    public static final String FASTER_HARVEST_THREAD_NAME = "New Relic Faster Harvest Service";
    private static final long INITIAL_DELAY_IN_MILLISECONDS = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
    private static final long REPORTING_PERIOD_IN_MILLISECONDS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS);
    private static final long MIN_HARVEST_INTERVAL_IN_NANOSECONDS = TimeUnit.NANOSECONDS.convert(55, TimeUnit.SECONDS);
    public static final String HARVEST_LIMITS = "harvest_limits";
    private static final String REPORT_PERIOD_MS = "report_period_ms";

    /**
     * The main harvest task and all harvestables (faster event harvests) are scheduled on separate threads.
     */
    private final ScheduledExecutorService scheduledHarvestExecutor;
    private final ScheduledExecutorService scheduledFasterHarvestExecutor;
    private final List<HarvestListener> harvestListeners = new CopyOnWriteArrayList<>();
    private final Map<IRPMService, HarvestTask> harvestTasks = new HashMap<>();
    private final ConcurrentMap<Harvestable, HarvestableTracker> harvestables = new ConcurrentHashMap<>();
    private long overrideInitialDelay = -1;

    public HarvestServiceImpl() {
        super(HarvestService.class.getSimpleName());
        scheduledHarvestExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(HARVEST_THREAD_NAME, true));
        scheduledFasterHarvestExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(FASTER_HARVEST_THREAD_NAME, true));
        ServiceFactory.getRPMServiceManager().addConnectionListener(new ConnectionListenerImpl());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
    }

    /**
     * Start a harvest task for the RPM service (or restart it if the reporting period has changed).
     */
    @Override
    public void startHarvest(IRPMService rpmService) {
        HarvestTask harvestTask = getOrCreateHarvestTask(rpmService);
        harvestTask.start();
    }

    @VisibleForTesting
    public void startHarvestables(IRPMService rpmService, AgentConfig config) {
        Map<String, Object> eventHarvestConfig = config.getProperty(AgentConfigFactory.EVENT_HARVEST_CONFIG);
        Map<String, Object> spanHarvestConfig = config.getProperty(SERVER_SPAN_HARVEST_CONFIG);

        if (eventHarvestConfig == null) {
            ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(
                    MetricNames.SUPPORTABILITY_CONNECT_MISSING_EVENT_DATA, 1), MetricNames.SUPPORTABILITY_CONNECT_MISSING_EVENT_DATA);
        }

        for (HarvestableTracker tracker : harvestables.values()) {
            if (tracker.harvestable.getAppName().equals(rpmService.getApplicationName())) {
                int maxSamplesStored = tracker.harvestable.getMaxSamplesStored();
                long reportPeriodInMillis = HarvestServiceImpl.REPORTING_PERIOD_IN_MILLISECONDS;
                boolean isSpanEventEndpoint = tracker.harvestable.getEndpointMethodName().equals(SPAN_EVENT_DATA);

                // The event_harvest_config.harvest_limits received from server-side during the connect lifecycle
                // contains config for error_event_data, analytic_event_data, custom_event_data, and log_event_data
                if (eventHarvestConfig != null && !isSpanEventEndpoint) {
                    Agent.LOG.log(Level.FINE, "event_harvest_config from collector for {0} is: {1} max samples stored per minute",
                            tracker.harvestable.getEndpointMethodName(), maxSamplesStored);
                    Map<String, Object> harvestLimits = (Map<String, Object>) eventHarvestConfig.get(HARVEST_LIMITS);

                    Long harvestLimit = (Long) harvestLimits.get(tracker.harvestable.getEndpointMethodName());
                    if (harvestLimit != null) {
                        maxSamplesStored = harvestLimit.intValue();
                        reportPeriodInMillis = (long) eventHarvestConfig.get(REPORT_PERIOD_MS); // faster event harvest report period
                        float reportPeriodInSeconds = reportPeriodInMillis / 1000;
                        if (maxSamplesStored == 0) {
                            Agent.LOG.log(Level.INFO, "harvest limit has been disabled by the collector for {0}", tracker.harvestable.getEndpointMethodName());
                        }
                        Agent.LOG.log(Level.FINE, "harvest limit from collector for {0} is: {1} max samples stored per every {2} second harvest",
                                tracker.harvestable.getEndpointMethodName(), harvestLimit, reportPeriodInSeconds);
                        ServiceFactory.getStatsService().doStatsWork(
                                StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_EVENT_HARVEST_REPORT_PERIOD_IN_SECONDS, reportPeriodInSeconds),
                                MetricNames.SUPPORTABILITY_EVENT_HARVEST_REPORT_PERIOD_IN_SECONDS);
                    }
                } else if (!isSpanEventEndpoint) {
                    Agent.LOG.log(Level.FINE, "event_harvest_config from collector for {0} was null. Using default value: {1} max samples stored per minute",
                            tracker.harvestable.getEndpointMethodName(), maxSamplesStored);
                }

                // The span_event_harvest_config received from server-side during the connect lifecycle contains config for span_event_data
                if (spanHarvestConfig != null && isSpanEventEndpoint) {
                    Agent.LOG.log(Level.FINE, "span_event_harvest_config from collector for {0} is: {1} max samples stored per minute",
                            tracker.harvestable.getEndpointMethodName(), maxSamplesStored);
                    Long harvestLimit = (Long) spanHarvestConfig.get(SERVER_SPAN_HARVEST_LIMIT);
                    if (harvestLimit != null) {
                        maxSamplesStored = harvestLimit.intValue();
                        reportPeriodInMillis = (long) spanHarvestConfig.get(REPORT_PERIOD_MS);
                        float reportPeriodInSeconds = reportPeriodInMillis / 1000;
                        Agent.LOG.log(Level.FINE, "harvest limit from collector for {0} is: {1} max samples stored per every {2} second harvest",
                                tracker.harvestable.getEndpointMethodName(), harvestLimit, reportPeriodInSeconds);
                    }
                } else if (isSpanEventEndpoint) {
                    Agent.LOG.log(Level.FINE,
                            "span_event_harvest_config from collector for {0} was null. Using default value: {1} max samples stored per minute",
                            tracker.harvestable.getEndpointMethodName(), maxSamplesStored);
                }

                tracker.start(reportPeriodInMillis, maxSamplesStored);
            }
        }
    }

    /**
     * Stop a harvest task for the RPM service
     */
    public void stopHarvest(IRPMService rpmService) {
        HarvestTask harvestTask = harvestTasks.remove(rpmService);
        if (harvestTask != null) {
            harvestTask.stop();
        }
    }

    private synchronized HarvestTask getOrCreateHarvestTask(IRPMService rpmService) {
        HarvestTask harvestTask = harvestTasks.get(rpmService);
        if (harvestTask == null) {
            harvestTask = new HarvestTask(rpmService);
            harvestTasks.put(rpmService, harvestTask);
        }
        return harvestTask;
    }

    private synchronized List<HarvestTask> getHarvestTasks() {
        return new ArrayList<>(harvestTasks.values());
    }

    @Override
    public void addHarvestable(final Harvestable harvestable) {
        HarvestableTracker existing = harvestables.putIfAbsent(harvestable, new HarvestableTracker(harvestable));
        if (existing != null) {
            Agent.LOG.log(Level.SEVERE, "Harvestable already added to the harvest service: {0}", harvestable);
            existing.stop();
        }
    }

    @Override
    public void removeHarvestable(Harvestable harvestable) {
        if (harvestable != null) {
            HarvestableTracker tracker = harvestables.remove(harvestable);
            if (tracker != null) {
                tracker.stop();
            }
        }
    }

    @Override
    public void removeHarvestablesByAppName(String appName) {
        for (HarvestableTracker tracker : harvestables.values()) {
            if (tracker.harvestable.getAppName().equals(appName)) {
                harvestables.remove(tracker.harvestable);
                if (tracker != null) {
                    tracker.stop();
                }
            }
        }
    }

    /**
     * Add a listener that will be called by the harvest tasks before and after metric data is sent.
     *
     * @param listener
     */
    @Override
    public void addHarvestListener(HarvestListener listener) {
        harvestListeners.add(listener);
    }

    /**
     * Remove a listener.
     *
     * @param listener
     */
    @Override
    public void removeHarvestListener(HarvestListener listener) {
        harvestListeners.remove(listener);
    }

    @Override
    protected void doStop() {
        List<HarvestTask> tasks = getHarvestTasks();
        for (HarvestTask task : tasks) {
            task.stop();
        }
        for (HarvestableTracker h : harvestables.values()) {
            h.stop();
        }
        scheduledHarvestExecutor.shutdown();
        scheduledFasterHarvestExecutor.shutdown();
    }

    /**
     * Schedule a harvest task.
     */
    private ScheduledFuture<?> scheduleHarvestTask(HarvestTask harvestTask) {
        return scheduledHarvestExecutor.scheduleAtFixedRate(SafeWrappers.safeRunnable(harvestTask), getInitialDelay(),
                getReportingPeriod(), TimeUnit.MILLISECONDS);
    }

    /**
     * Get the initial delay in milliseconds.
     * <p>
     * Tests can override.
     */
    public long getInitialDelay() {
        return (overrideInitialDelay <= 0) ? INITIAL_DELAY_IN_MILLISECONDS : overrideInitialDelay;
    }

    /**
     * Override the initial delay.
     *
     * @param millis number of milliseconds to wait
     */
    @VisibleForTesting
    public void setInitialDelayMillis(long millis) {
        this.overrideInitialDelay = millis;
    }

    /**
     * Get the reporting period in milliseconds.
     * <p>
     * Tests can override.
     */
    public long getReportingPeriod() {
        return REPORTING_PERIOD_IN_MILLISECONDS;
    }

    /**
     * Get the minimum harvest interval in nanoseconds.
     * <p>
     * Tests can override.
     */
    public long getMinHarvestInterval() {
        return MIN_HARVEST_INTERVAL_IN_NANOSECONDS;
    }

    /**
     * Run harvests now.
     */
    @Override
    public void harvestNow() {
        List<HarvestTask> tasks = getHarvestTasks();
        for (HarvestTask task : tasks) {
            // run the harvestables first - they often generate metric data
            for (HarvestableTracker h : harvestables.values()) {
                h.harvestable.harvest();
            }
            // now run the normal harvest
            task.harvestNow();
        }
    }

    public Map<String, Object> getEventDataHarvestLimits() {
        Map<String, Object> eventHarvest = new HashMap<>();
        Map<String, Object> harvestLimits = new HashMap<>();
        eventHarvest.put(HARVEST_LIMITS, harvestLimits);

        for (Harvestable harvestable : harvestables.keySet()) {
            harvestLimits.put(harvestable.getEndpointMethodName(), harvestable.getMaxSamplesStored());
        }
        return eventHarvest;
    }

    /**
     * The harvest task is responsible for running the harvest for a RPM service.
     */
    private final class HarvestTask implements Runnable {

        private final IRPMService rpmService;
        private ScheduledFuture<?> task;
        private final Lock harvestLock = new ReentrantLock();
        private StatsEngine lastStatsEngine = new StatsEngineImpl();
        private long lastHarvestStartTime;

        private HarvestTask(IRPMService rpmService) {
            this.rpmService = rpmService;
        }

        @Override
        public void run() {
            try {
                if (shouldHarvest()) {
                    harvest();
                }
            } catch (Throwable t) {
                String msg = MessageFormat.format("Unexpected exception during harvest: {0}", t);
                if (getLogger().isLoggable(Level.FINER)) {
                    getLogger().log(Level.WARNING, msg, t);
                } else {
                    getLogger().warning(msg);
                }
            }
        }

        private boolean shouldHarvest() {
            return (System.nanoTime() - lastHarvestStartTime) >= getMinHarvestInterval();
        }

        /**
         * Start the harvest task if it is not running.
         */
        private synchronized void start() {
            if (!isRunning()) {
                stop();
                String msg = MessageFormat.format("Scheduling harvest task for {0}", rpmService.getApplicationName());
                getLogger().log(Level.FINE, msg);
                task = scheduleHarvestTask(this);
            }
        }

        /**
         * Stop the harvest task.
         */
        private synchronized void stop() {
            if (task != null) {
                getLogger().fine(MessageFormat.format("Cancelling harvest task for {0}", rpmService.getApplicationName()));
                task.cancel(false);
            }
        }

        /**
         * Is the harvest task running.
         *
         * @return <tt>true</tt> if the harvest task is running, <tt>false</tt> otherwise
         */
        private boolean isRunning() {
            if (task == null) {
                return false;
            }
            return !task.isCancelled() || task.isDone();
        }

        /**
         * Do a harvest now.
         */
        private void harvestNow() {
            if (rpmService.isConnected()) {
                String msg = MessageFormat.format("Sending metrics for {0} immediately", rpmService.getApplicationName());
                getLogger().info(msg);
                harvest();
            }
        }

        private void harvest() {
            harvestLock.lock();
            try {
                doHarvest();
            } catch (ServerCommandException | IgnoreSilentlyException e) {
            } catch (Throwable e) {
                getLogger().log(Level.INFO, "Error sending metric data for {0}: {1}", rpmService.getApplicationName(), e.toString());
            } finally {
                harvestLock.unlock();
            }
        }

        private void doHarvest() throws Exception {
            lastHarvestStartTime = System.nanoTime();
            String appName = rpmService.getApplicationName();
            if (getLogger().isLoggable(Level.FINE)) {
                String msg = MessageFormat.format("Starting harvest for {0}", appName);
                getLogger().fine(msg);

                // Returns the application link and version at every harvest cycle. Helps verify customer agent
                // config when finest logs is sent to Support/Dev without a restart.
                String linkText = rpmService.getApplicationLink();
                String version = Agent.getVersion();
                String reportingToAndVersion = MessageFormat.format("Application link: {0}, Agent version: {1}", linkText, version);
                getLogger().fine(reportingToAndVersion);
            }

            StatsEngine harvestStatsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(appName);
            harvestStatsEngine.mergeStats(lastStatsEngine);
            try {
                for (HarvestListener listener : harvestListeners) {
                    notifyListenerBeforeHarvest(appName, harvestStatsEngine, listener);
                }
                // RPMService metric harvest only, other harvests are triggered in the before/after harvest listener loops
                reportHarvest(appName, harvestStatsEngine, rpmService);

                for (HarvestListener listener : harvestListeners) {
                    notifyListenerAfterHarvest(appName, listener);
                }
            } finally {
                if (harvestStatsEngine.getSize() > MetricIdRegistry.METRIC_LIMIT) {
                    harvestStatsEngine.clear();
                }
                lastStatsEngine = harvestStatsEngine;
                long duration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastHarvestStartTime,
                        TimeUnit.NANOSECONDS);
                harvestStatsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_HARVEST_SERVICE_RESPONSE_TIME)
                        .recordResponseTime(duration, TimeUnit.MILLISECONDS);
                if (getLogger().isLoggable(Level.FINE)) {
                    String msg = MessageFormat.format("Harvest for {0} took {1} milliseconds", appName, duration);
                    getLogger().fine(msg);
                }
            }
        }
    }

    private void reportHarvest(String appName, StatsEngine statsEngine, IRPMService rpmService) {
        try {
            rpmService.harvest(statsEngine);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error reporting harvest data for {0}: {1}", appName, e);
            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            } else {
                getLogger().finer(msg);
            }
        }
    }

    private void notifyListenerBeforeHarvest(String appName, StatsEngine statsEngine, HarvestListener listener) {
        try {
            listener.beforeHarvest(appName, statsEngine);
        } catch (Throwable e) {
            String msg = MessageFormat.format("Error harvesting data for {0}: {1}", appName, e);
            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            } else {
                getLogger().finer(msg);
            }
        }
    }

    private void notifyListenerAfterHarvest(String appName, HarvestListener listener) {
        try {
            listener.afterHarvest(appName);
        } catch (Throwable e) {
            String msg = MessageFormat.format("Error harvesting data for {0}: {1}", appName, e);
            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            } else {
                getLogger().finer(msg);
            }
        }
    }

    private class HarvestableTracker {
        private final Harvestable harvestable;
        private final List<ScheduledFuture<?>> tasks = new ArrayList<>();

        public HarvestableTracker(Harvestable harvestable) {
            this.harvestable = harvestable;
        }

        public synchronized void start(long reportPeriodInMillis, int maxSamplesStored) {
            stop();
            harvestable.configure(reportPeriodInMillis, maxSamplesStored);

            Runnable harvestTask = new Runnable() {
                @Override
                public void run() {
                    if (harvestable.getEndpointMethodName().equals("span_event_data")) {
                        getLogger().log(Level.FINER, "*SpanEvent*  Harvestable: {0}/{1} running", harvestable.getAppName(), harvestable.getEndpointMethodName());
                    }
                    harvestable.harvest();
                }
            };

            tasks.add(
                    scheduledFasterHarvestExecutor.scheduleAtFixedRate(SafeWrappers.safeRunnable(harvestTask), 0, reportPeriodInMillis, TimeUnit.MILLISECONDS));
        }

        public synchronized void stop() {
            for (ScheduledFuture<?> task : tasks) {
                task.cancel(false);
            }

            tasks.clear();
        }
    }

    private class ConnectionListenerImpl implements ConnectionListener {
        @Override
        public void connected(IRPMService rpmService, AgentConfig agentConfig) {
            startHarvest(rpmService);
            startHarvestables(rpmService, agentConfig);
        }

        @Override
        public void disconnected(IRPMService rpmService) {
            for (HarvestableTracker h : harvestables.values()) {
                h.stop();
            }
        }
    }

}
