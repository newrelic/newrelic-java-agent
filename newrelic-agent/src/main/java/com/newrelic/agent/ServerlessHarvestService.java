/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Serverless implementation of HarvestService that performs immediate harvest on demand
 * rather than periodic scheduled harvests.
 */
public class ServerlessHarvestService extends AbstractService implements HarvestService {

    public static final String HARVEST_LIMITS = "harvest_limits";

    /**
     * Default report period for configuration purposes.
     * In serverless mode, this value is not used for scheduling (harvests occur on transaction completion),
     * but is passed to harvestables for supportability metric recording.
     */
    private static final long DEFAULT_REPORT_PERIOD_MS = 60_000;

    private final List<HarvestListener> harvestListeners = new CopyOnWriteArrayList<>();
    private final Map<IRPMService, ServerlessHarvestTask> harvestTasks = new HashMap<>();
    private final Set<Harvestable> harvestables = ConcurrentHashMap.newKeySet();

    public ServerlessHarvestService() {
        super(HarvestService.class.getSimpleName());
        ServiceFactory.getRPMServiceManager().addConnectionListener(new ConnectionListenerImpl());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
        // No scheduled executors to start in serverless mode
        getLogger().log(Level.FINE, "Starting ServerlessHarvestService - harvest will occur on demand");
    }

    @Override
    protected void doStop() {
        synchronized (this) {
            harvestTasks.clear();
        }
        harvestables.clear();
        getLogger().log(Level.FINE, "Stopped ServerlessHarvestService");
    }

    /**
     * Start a harvest task for the RPM service.
     * In serverless mode, this creates the task but doesn't schedule it.
     */
    @Override
    public void startHarvest(IRPMService rpmService) {
        synchronized (this) {
            ServerlessHarvestTask harvestTask = harvestTasks.get(rpmService);
            if (harvestTask == null) {
                harvestTask = new ServerlessHarvestTask(rpmService);
                harvestTasks.put(rpmService, harvestTask);
                getLogger().log(Level.FINE, "Created harvest task for {0}", rpmService.getApplicationName());
            }
        }
    }

    /**
     * Configure harvestables with their limits.
     * In serverless mode, harvestables are not scheduled but server-provided limits are respected.
     */
    public void startHarvestables(IRPMService rpmService, AgentConfig config) {
        Map<String, Object> eventHarvestConfig = config.getProperty(AgentConfigFactory.EVENT_HARVEST_CONFIG);

        for (Harvestable harvestable : harvestables) {
            if (harvestable.getAppName().equals(rpmService.getApplicationName())) {
                int maxSamplesStored = harvestable.getMaxSamplesStored();

                // Apply server-provided limits if available
                if (eventHarvestConfig != null) {
                    Map<String, Object> harvestLimits = (Map<String, Object>) eventHarvestConfig.get(HARVEST_LIMITS);
                    if (harvestLimits != null) {
                        Long serverLimit = (Long) harvestLimits.get(harvestable.getEndpointMethodName());
                        if (serverLimit != null) {
                            maxSamplesStored = serverLimit.intValue();
                            getLogger().log(Level.FINE, "Using server-provided limit {0} for {1}",
                                    maxSamplesStored, harvestable.getEndpointMethodName());
                        }
                    }
                }

                harvestable.configure(DEFAULT_REPORT_PERIOD_MS, maxSamplesStored);
                getLogger().log(Level.FINE, "Configured harvestable {0} for serverless mode with limit {1}",
                        harvestable.getEndpointMethodName(), maxSamplesStored);
            }
        }
    }

    @Override
    public void stopHarvest(IRPMService rpmService) {
        synchronized (this) {
            harvestTasks.remove(rpmService);
        }
    }

    @Override
    public void addHarvestable(final Harvestable harvestable) {
        if (!harvestables.add(harvestable)) {
            getLogger().log(Level.SEVERE, "Harvestable already added to the harvest service: {0}", harvestable);
        }
    }

    @Override
    public void removeHarvestable(Harvestable harvestable) {
        if (harvestable != null) {
            harvestables.remove(harvestable);
        }
    }

    @Override
    public void removeHarvestablesByAppName(String appName) {
        harvestables.removeIf(harvestable -> harvestable.getAppName().equals(appName));
    }

    @Override
    public void addHarvestListener(HarvestListener listener) {
        harvestListeners.add(listener);
    }

    @Override
    public void removeHarvestListener(HarvestListener listener) {
        harvestListeners.remove(listener);
    }

    /**
     * Perform an immediate harvest. This is the main entry point for serverless harvests,
     * called when a transaction completes.
     */
    @Override
    public void harvestNow() {
        getLogger().log(Level.FINEST, "Serverless mode: Beginning harvest cycle");

        List<ServerlessHarvestTask> tasks;
        synchronized (this) {
            tasks = new ArrayList<>(harvestTasks.values());
        }

        for (ServerlessHarvestTask task : tasks) {
            for (Harvestable harvestable : harvestables) {
                try {
                    harvestable.harvest();
                } catch (Throwable t) {
                    getLogger().log(Level.WARNING, "Error harvesting {0}: {1}",
                            harvestable.getEndpointMethodName(), t.toString());
                }
            }

            task.harvestNow();
        }
    }

    @Override
    public Map<String, Object> getEventDataHarvestLimits() {
        Map<String, Object> eventHarvest = new HashMap<>();
        Map<String, Object> harvestLimits = new HashMap<>();
        eventHarvest.put(HARVEST_LIMITS, harvestLimits);

        for (Harvestable harvestable : harvestables) {
            harvestLimits.put(harvestable.getEndpointMethodName(), harvestable.getMaxSamplesStored());
        }
        return eventHarvest;
    }

    /**
     * Harvest task for serverless mode that performs immediate harvest without scheduling.
     * Uses ReentrantLock to serialize harvest execution and protect shared state.
     */
    private class ServerlessHarvestTask {
        private final IRPMService rpmService;
        private final Lock harvestLock = new ReentrantLock();
        private StatsEngine lastStatsEngine = new StatsEngineImpl();

        private ServerlessHarvestTask(IRPMService rpmService) {
            this.rpmService = rpmService;
        }

        /**
         * Perform an immediate harvest.
         * Acquires lock to ensure only one harvest executes at a time per RPM service.
         * This prevents race conditions on lastStatsEngine and protects against data corruption.
         */
        private void harvestNow() {
            if (!rpmService.isConnected()) {
                getLogger().log(Level.FINE, "Skipping harvest for {0} - not connected",
                        rpmService.getApplicationName());
                return;
            }

            harvestLock.lock();
            try {
                doHarvest();
            } catch (Throwable t) {
                String msg = MessageFormat.format("Error during serverless harvest for {0}: {1}",
                        rpmService.getApplicationName(), t.toString());
                if (getLogger().isLoggable(Level.FINER)) {
                    getLogger().log(Level.WARNING, msg, t);
                } else {
                    getLogger().warning(msg);
                }
            } finally {
                harvestLock.unlock();
            }
        }

        private void doHarvest() throws Exception {
            long startTime = System.nanoTime();
            String appName = rpmService.getApplicationName();

            getLogger().log(Level.FINEST, "Starting serverless harvest for {0}", appName);

            StatsEngine harvestStatsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(appName);
            harvestStatsEngine.mergeStats(lastStatsEngine);

            try {
                for (HarvestListener listener : harvestListeners) {
                    notifyListenerBeforeHarvest(appName, harvestStatsEngine, listener);
                }

                // Perform the harvest
                reportHarvest(appName, harvestStatsEngine, rpmService);

                for (HarvestListener listener : harvestListeners) {
                    notifyListenerAfterHarvest(appName, listener);
                }

                rpmService.commitAndFlush();
            } finally {
                if (harvestStatsEngine.getSize() > MetricIdRegistry.METRIC_LIMIT) {
                    harvestStatsEngine.clear();
                }
                lastStatsEngine = harvestStatsEngine;
                long duration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

                harvestStatsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_HARVEST_SERVICE_RESPONSE_TIME)
                        .recordResponseTime(duration, TimeUnit.MILLISECONDS);

                getLogger().log(Level.FINEST, "Serverless harvest for {0} completed in {1} milliseconds",
                        appName, duration);
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
                String msg = MessageFormat.format("Error in beforeHarvest for {0}: {1}", appName, e);
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
                String msg = MessageFormat.format("Error in afterHarvest for {0}: {1}", appName, e);
                if (getLogger().isLoggable(Level.FINER)) {
                    getLogger().log(Level.FINER, msg, e);
                } else {
                    getLogger().finer(msg);
                }
            }
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
            stopHarvest(rpmService);
        }
    }
}
