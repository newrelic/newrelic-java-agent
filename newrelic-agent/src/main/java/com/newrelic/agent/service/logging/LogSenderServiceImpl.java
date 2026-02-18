/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.logging;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentLinkingMetadata;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.Harvestable;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.TraceMetadataImpl;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.attributes.DisabledExcludeIncludeFilter;
import com.newrelic.agent.attributes.ExcludeIncludeFilter;
import com.newrelic.agent.attributes.ExcludeIncludeFilterImpl;
import com.newrelic.agent.attributes.LogAttributeValidator;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ApplicationLoggingConfig;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.util.NoOpQueue;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.Logs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.newrelic.agent.model.LogEvent.LOG_EVENT_TYPE;

public class LogSenderServiceImpl extends AbstractService implements LogSenderService {
    // Whether the service as a whole is enabled. Disabling shuts down all log events.
    private volatile boolean forwardingEnabled;
    // Whether the labels are enabled for log events
    private static boolean logLabelsEnabled;
    // Labels to be added to log events
    private static Map<String, String> labels;
    // Denylist of log levels that should be blocked from being stored
    private volatile Set<String> logLevelDenylist;
    // Key is the app name, value is if it is enabled - should be a limited number of names
    private final ConcurrentMap<String, Boolean> isEnabledForApp = new ConcurrentHashMap<>();
    // Number of log events in the reservoir sampling buffer per-app. All apps get the same value.
    private volatile int maxSamplesStored;
    // Number of millis between harvest/reporting cycles.
    // when the maxSamplesStores changes from the config file, it is set as per minute.  This value is needed
    // to properly calculate the per harvest cycle maxSamplesStored
    // we'll default to 5000, unless overridden
    volatile long reportPeriodInMillis = 5000;
    // Key is app name, value is collection of per-transaction log events for next harvest for that app.
    private final ConcurrentHashMap<String, DistributedSamplingPriorityQueue<LogEvent>> reservoirForApp = new ConcurrentHashMap<>();

    private static final LoadingCache<String, String> stringCache = Caffeine.newBuilder().maximumSize(1000)
            .expireAfterAccess(70, TimeUnit.SECONDS).executor(Runnable::run).build(key -> key);

    public static final String METHOD = "add log event attribute";
    public static final String LOG_SENDER_SERVICE = "Log Sender Service";

    private volatile ExcludeIncludeFilter contextDataKeyFilter;

    /**
     * Lifecycle listener for log events associated with a transaction
     */
    protected final ExtendedTransactionListener transactionListener = new ExtendedTransactionListener() {
        @Override
        public void dispatcherTransactionStarted(Transaction transaction) {
        }

        @Override
        public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
            // Get log events from the transaction when it is finished
            TransactionLogs data = (TransactionLogs) transactionData.getLogEventData();
            storeEvents(transactionData.getApplicationName(), transactionData.getPriority(), data.events);
        }

        @Override
        public void dispatcherTransactionCancelled(Transaction transaction) {
            // Get log events from the transaction when it is canceled
            // Even if the transaction is canceled we still want to send up any events that were held in it
            TransactionLogs data = (TransactionLogs) transaction.getLogEventData();
            storeEvents(transaction.getApplicationName(), transaction.getPriority(), data.events);
        }
    };

    /**
     * Listener to detect changes to the agent config
     */
    protected final AgentConfigListener configListener = new AgentConfigListener() {
        @Override
        public void configChanged(String appName, AgentConfig agentConfig) {
            ApplicationLoggingConfig appLoggingConfig = agentConfig.getApplicationLoggingConfig();

            // if the config has changed for the app, just remove it and regenerate enabled next transaction
            isEnabledForApp.remove(appName);

            maxSamplesStored = (int) (appLoggingConfig.getMaxSamplesStored()*(reportPeriodInMillis / 60000.0));
            forwardingEnabled = appLoggingConfig.isForwardingEnabled();
            contextDataKeyFilter = createContextDataKeyFilter(appLoggingConfig);
            logLevelDenylist = appLoggingConfig.getLogLevelDenylist();

            boolean metricsEnabled = appLoggingConfig.isMetricsEnabled();
            boolean localDecoratingEnabled = appLoggingConfig.isLocalDecoratingEnabled();
            recordApplicationLoggingSupportabilityMetrics(forwardingEnabled, metricsEnabled, localDecoratingEnabled, logLabelsEnabled);
        }
    };

    public void recordApplicationLoggingSupportabilityMetrics(boolean forwardingEnabled, boolean metricsEnabled, boolean localDecoratingEnabled, boolean logLabelsEnabled) {
        StatsService statsService = ServiceFactory.getServiceManager().getStatsService();

        if (forwardingEnabled) {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_FORWARDING_JAVA_ENABLED);
        } else {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_FORWARDING_JAVA_DISABLED);
        }

        if (metricsEnabled) {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_METRICS_JAVA_ENABLED);
        } else {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_METRICS_JAVA_DISABLED);
        }

        if (localDecoratingEnabled) {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_LOCAL_DECORATING_JAVA_ENABLED);
        } else {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_LOCAL_DECORATING_JAVA_DISABLED);
        }

        if (logLabelsEnabled) {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_LABELS_JAVA_ENABLED);
        } else {
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LOGGING_LABELS_JAVA_DISABLED);
        }
    }

    private final List<Harvestable> harvestables = new ArrayList<>();

    public LogSenderServiceImpl() {
        super(LogSenderServiceImpl.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        ApplicationLoggingConfig appLoggingConfig = config.getApplicationLoggingConfig();

        maxSamplesStored = (int) (appLoggingConfig.getMaxSamplesStored()*(reportPeriodInMillis / 60000.0));
        forwardingEnabled = appLoggingConfig.isForwardingEnabled();
        contextDataKeyFilter = createContextDataKeyFilter(appLoggingConfig);
        logLabelsEnabled = appLoggingConfig.isLogLabelsEnabled();
        labels = appLoggingConfig.removeExcludedLogLabels(config.getLabelsConfig().getLabels());
        logLevelDenylist = appLoggingConfig.getLogLevelDenylist();

        isEnabledForApp.put(config.getApplicationName(), forwardingEnabled);
    }

    private ExcludeIncludeFilter createContextDataKeyFilter(ApplicationLoggingConfig appLoggingConfig) {
        if (appLoggingConfig.isForwardingContextDataEnabled()) {
            List<String> include = appLoggingConfig.getForwardingContextDataInclude();
            List<String> exclude = appLoggingConfig.getForwardingContextDataExclude();
            return new ExcludeIncludeFilterImpl("application_logging.forwarding.context_data", exclude, include);
        } else {
            return DisabledExcludeIncludeFilter.INSTANCE;
        }
    }

    /**
     * Whether the LogSenderService is enabled or not
     *
     * @return true if enabled, else false
     */
    @Override
    public boolean isEnabled() {
        return forwardingEnabled;
    }

    /**
     * Start the LogSenderService
     * @throws Exception if service fails to start
     */
    @Override
    protected void doStart() throws Exception {
        // Register transaction listener to associate log events with transaction lifecycle
        ServiceFactory.getTransactionService().addTransactionListener(transactionListener);
        ServiceFactory.getConfigService().addIAgentConfigListener(configListener);
    }

    /**
     * Stop the LogSenderService
     * @throws Exception if service fails to stop
     */
    @Override
    protected void doStop() throws Exception {
        removeHarvestables();
        ServiceFactory.getTransactionService().removeTransactionListener(transactionListener);
        ServiceFactory.getConfigService().removeIAgentConfigListener(configListener);
        reservoirForApp.clear();
        isEnabledForApp.clear();
        stringCache.invalidateAll();
    }

    private void removeHarvestables() {
        for (Harvestable harvestable : harvestables) {
            ServiceFactory.getHarvestService().removeHarvestable(harvestable);
        }
    }

    /**
     * Records a LogEvent. If a LogEvent occurs within a Transaction it will be associated with it.
     *
     * @param attributes A map of log event data (e.g. log message, log timestamp, log level)
     *                   Each key should be a String and each value should be a String, Number, or Boolean.
     *                   For map values that are not String, Number, or Boolean object types the toString value will be used.
     */
    @Override
    public void recordLogEvent(Map<LogAttributeKey, ?> attributes) {
        if (logEventsDisabled() || attributes == null || attributes.isEmpty() || shouldDenyLogLevel(attributes)) {
            return;
        }

        Transaction transaction = ServiceFactory.getTransactionService().getTransaction(false);
        // Not in a Transaction or an existing Transaction is not in progress or is ignored
        if (transaction == null || !transaction.isInProgress() || transaction.isIgnore()) {
            String applicationName = ServiceFactory.getRPMService().getApplicationName();

            if (transaction != null && transaction.getApplicationName() != null) {
                applicationName = transaction.getApplicationName();
            }

            AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(applicationName);

            if (!getIsEnabledForApp(agentConfig, applicationName)) {
                reservoirForApp.remove(applicationName);
                return;
            }
            createAndStoreEvent(applicationName, attributes);
        // In a Transaction that is in progress and not ignored
        } else {
            // Store log events on the transaction
            transaction.getLogEventData().recordLogEvent(attributes);
        }
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_RECORD_LOG_EVENT);
    }

    /**
     * Store a collection of LogEvents in the priority queue when a Transaction is finished or cancelled
     *
     * @param appName app name
     * @param priority sampling priority from Transaction
     * @param events collection of LogEvents to store
     */
    private void storeEvents(String appName, float priority, Collection<LogEvent> events) {
        if (events.size() > 0) {
            DistributedSamplingPriorityQueue<LogEvent> eventList = getReservoir(appName);
            for (LogEvent event : events) {
                // Set "priority" on LogEvent based on priority value from Transaction
                event.setPriority(priority);
                eventList.add(event);
            }
        }
    }

    /**
     * Register LogSenderHarvestable
     * @param appName application name
     */
    public void addHarvestableToService(String appName) {
        Harvestable harvestable = new LogSenderHarvestableImpl(this, appName);
        ServiceFactory.getHarvestService().addHarvestable(harvestable);
        harvestables.add(harvestable);
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public void setMaxSamplesStored(int maxSamplesStored) {
        this.maxSamplesStored = maxSamplesStored;
    }

    public void setReportPeriodInMillis(long reportPeriodInMillis) {
        this.reportPeriodInMillis = reportPeriodInMillis;
    }

    public void clearReservoir() {
        reservoirForApp.clear();
    }

    public void clearReservoir(String appName) {
        DistributedSamplingPriorityQueue<LogEvent> reservoir = reservoirForApp.get(appName);
        if (reservoir != null) {
            reservoir.clear();
        }
    }

    @VisibleForTesting
    void configureHarvestables(long reportPeriodInMillis, int maxSamplesStored) {
        for (Harvestable h : harvestables) {
            h.configure(reportPeriodInMillis, maxSamplesStored);
        }
    }

    @VisibleForTesting
    public void harvestHarvestables() {
        for (Harvestable h : harvestables) {
            h.harvest();
        }
    }

    public void harvestPendingEvents() {
        // harvest pending events
        for (String appName : reservoirForApp.keySet()) {
            harvestEvents(appName);
        }
    }

    /**
     * Store a LogEvent instance
     * @param appName application name
     * @param event log event
     */
    @Override
    public void storeEvent(String appName, LogEvent event) {
        if (logEventsDisabled()) {
            return;
        }

        DistributedSamplingPriorityQueue<LogEvent> eventList = getReservoir(appName);
        eventList.add(event);
        Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0}", event.getType()));
    }

    /**
     * Create and store a LogEvent instance
     *
     * @param appName    application name
     * @param attributes Map of attributes to create a LogEvent from
     */
    private void createAndStoreEvent(String appName, Map<LogAttributeKey, ?> attributes) {
        if (logEventsDisabled()) {
            return;
        }
        DistributedSamplingPriorityQueue<LogEvent> eventList = getReservoir(appName);
        eventList.add(createValidatedEvent(attributes, contextDataKeyFilter));
        Agent.LOG.finest(MessageFormat.format("Added event of type {0}", LOG_EVENT_TYPE));
    }

    /**
     * Check if LogEvents are disabled
     *
     * @return true if they are disabled, false if they are enabled
     */
    private boolean logEventsDisabled() {
        if (!forwardingEnabled) {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected due to high security mode being enabled.", LOG_EVENT_TYPE);
            } else {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected. application_logging.forwarding not enabled.", LOG_EVENT_TYPE);
            }
            Agent.LOG.log(Level.FINER, "Event of type {0} not collected. application_logging.forwarding not enabled.", LOG_EVENT_TYPE);
            return true; // LogEvents are disabled
        }
        return false; // LogEvents are enabled
    }

    /**
     * Check the attribute map to see if the "level" attribute matches any level in the denylist.
     *
     * @return true if the "level" attribute in this map has a value that appears in the configured denylist, false otherwise.
     */
    private boolean shouldDenyLogLevel(Map<LogAttributeKey, ?> attributes) {
        Object level = attributes.get(AppLoggingUtils.LEVEL);
        if (level != null && !logLevelDenylist.isEmpty()) {
            String levelStr = level.toString().toUpperCase();
            return logLevelDenylist.contains(levelStr);
        }
        return false;
    }

    /**
     * Get the LogEvent reservoir
     *
     * @param appName app name
     * @return Queue of LogEvent instances
     */
    @VisibleForTesting
    public DistributedSamplingPriorityQueue<LogEvent> getReservoir(String appName) {
        DistributedSamplingPriorityQueue<LogEvent> result = reservoirForApp.get(appName);
        while (result == null) {
            // I don't think this loop can actually execute more than once, but it's prudent to assume it can.
            reservoirForApp.putIfAbsent(appName, new DistributedSamplingPriorityQueue<>(appName, LOG_SENDER_SERVICE, maxSamplesStored));
            result = reservoirForApp.get(appName);
        }
        return result;
    }

    /**
     * Harvest and send the LogEvents
     *
     * @param appName the application to harvest for
     */
    public void harvestEvents(final String appName) {
        if (!getIsEnabledForApp(ServiceFactory.getConfigService().getAgentConfig(appName), appName)) {
            reservoirForApp.remove(appName);
            return;
        }
        if (maxSamplesStored <= 0) {
            clearReservoir(appName);
            return;
        }

        long startTimeInNanos = System.nanoTime();

        final DistributedSamplingPriorityQueue<LogEvent> reservoir = this.reservoirForApp.put(appName,
                new DistributedSamplingPriorityQueue<>(appName, LOG_SENDER_SERVICE, maxSamplesStored));

        if (reservoir != null && reservoir.size() > 0) {
            try {
                // Send LogEvents
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(appName)
                        .sendLogEvents(Collections.unmodifiableList(reservoir.asList()));

                final long durationInNanos = System.nanoTime() - startTimeInNanos;
                ServiceFactory.getStatsService().doStatsWork(new StatsWork() {
                    @Override
                    public void doWork(StatsEngine statsEngine) {
                        recordSupportabilityMetrics(statsEngine, durationInNanos, reservoir);
                    }

                    @Override
                    public String getAppName() {
                        return appName;
                    }
                }, LogSenderServiceImpl.class.getName());

                if (reservoir.size() < reservoir.getNumberOfTries()) {
                    int dropped = reservoir.getNumberOfTries() - reservoir.size();
                    Agent.LOG.log(Level.FINE, "Dropped {0} log events out of {1}.", dropped, reservoir.getNumberOfTries());
                }
            } catch (HttpError e) {
                if (!e.discardHarvestData()) {
                    Agent.LOG.log(Level.FINE, "Unable to send log events. Unsent events will be included in the next harvest.", e);
                    // Save unsent data by merging it with current data using reservoir algorithm
                    DistributedSamplingPriorityQueue<LogEvent> currentReservoir = reservoirForApp.get(appName);
                    currentReservoir.retryAll(reservoir);
                } else {
                    // discard harvest data
                    reservoir.clear();
                    Agent.LOG.log(Level.FINE, "Unable to send log events. Unsent events will be dropped.", e);
                }
            } catch (Exception e) {
                // discard harvest data
                reservoir.clear();
                Agent.LOG.log(Level.FINE, "Unable to send log events. Unsent events will be dropped.", e);
            }
        }
    }

    @Override
    public String getEventHarvestIntervalMetric() {
        return MetricNames.SUPPORTABILITY_LOG_SENDER_SERVICE_EVENT_HARVEST_INTERVAL;
    }

    @Override
    public String getReportPeriodInSecondsMetric() {
        return MetricNames.SUPPORTABILITY_LOG_SENDER_SERVICE_REPORT_PERIOD_IN_SECONDS;
    }

    @Override
    public String getEventHarvestLimitMetric() {
        return MetricNames.SUPPORTABILITY_LOG_EVENT_DATA_HARVEST_LIMIT;
    }

    private void recordSupportabilityMetrics(StatsEngine statsEngine, long durationInNanoseconds,
                                             DistributedSamplingPriorityQueue<LogEvent> reservoir) {
        statsEngine.getStats(MetricNames.SUPPORTABILITY_LOGGING_FORWARDING_SENT)
                .incrementCallCount(reservoir.size());
        statsEngine.getStats(MetricNames.SUPPORTABILITY_LOGGING_FORWARDING_SEEN)
                .incrementCallCount(reservoir.getNumberOfTries());

        int droppedLogEvents = reservoir.getNumberOfTries() - reservoir.size();
        if (droppedLogEvents >= 0) {
            statsEngine.getStats(MetricNames.LOGGING_FORWARDING_DROPPED).incrementCallCount(droppedLogEvents);
        } else {
            Agent.LOG.log(Level.FINE, "Invalid dropped log events value of {0}. This must be a non-negative value.", droppedLogEvents);
        }

        statsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_LOG_SENDER_SERVICE_EVENT_HARVEST_TRANSMIT)
                .recordResponseTime(durationInNanoseconds, TimeUnit.NANOSECONDS);
    }

    private boolean getIsEnabledForApp(AgentConfig config, String currentAppName) {
        Boolean appEnabled = currentAppName == null ? null : isEnabledForApp.get(currentAppName);
        if (appEnabled == null) {
            appEnabled = config.getApplicationLoggingConfig().isForwardingEnabled();
            isEnabledForApp.put(currentAppName, appEnabled);
        }
        return appEnabled;
    }

    /**
     * We put Strings that occur in events in a map so that we're only ever holding a reference to one byte array for
     * any given string. It's basically like interning the string without using a global map.
     *
     * @param value the string to "intern"
     * @return the interned string
     */
    private static String mapInternString(String value) {
        // Note that the interning occurs on the *input* to the validation code. If the validation code truncates or
        // otherwise replaces the "interned" string, the new string will not be "interned" by this cache. See the
        // comment below for more information.
        return stringCache.get(value);
    }

    /**
     * Create a validated LogEvent
     *
     * @param attributes           Map of attributes to create a LogEvent from
     * @param contextDataKeyFilter
     * @return LogEvent instance
     */
    private static LogEvent createValidatedEvent(Map<LogAttributeKey, ?> attributes, ExcludeIncludeFilter contextDataKeyFilter) {
        Map<String, String> logEventLinkingMetadata = AgentLinkingMetadata.getLogEventLinkingMetadata(TraceMetadataImpl.INSTANCE,
                ServiceFactory.getConfigService(), ServiceFactory.getRPMService());
        // Initialize new logEventAttributes map with agent linking metadata
        Map<String, Object> logEventAttributes = new HashMap<>(logEventLinkingMetadata);

        if (labels != null && logLabelsEnabled) {
            for (Map.Entry<String, String> label : labels.entrySet()) {
                String labelKey = "tags." + label.getKey();
                logEventAttributes.put(labelKey, label.getValue());
            }
        }

        LogEvent event = new LogEvent(logEventAttributes, DistributedTraceServiceImpl.nextTruncatedFloat());

        // Now add the attributes from the argument map to the event using an AttributeSender.
        // An AttributeSender is the way to reuse all the existing attribute validations. We
        // also locally "intern" Strings because we anticipate a lot of reuse of the keys and,
        // possibly, the values. But there's an interaction: if the key or value is chopped
        // within the attribute sender, the modified value won't be "interned" in our map.
        AttributeSender sender = new LogEventAttributeSender(logEventAttributes);

        for (Map.Entry<LogAttributeKey, ?> entry : attributes.entrySet()) {
            LogAttributeKey logAttrKey = entry.getKey();
            Object value = entry.getValue();
            String key = logAttrKey.getKey();

            // key or value is null, skip it with a log message and iterate to next entry in attributes.entrySet()
            if (key == null || value == null) {
                Agent.LOG.log(Level.FINEST, "Log event with invalid attributes key or value of null was reported for a transaction."
                        + " This attribute will be ignored. Each key should be a String and each value should be a String, Number, or Boolean."
                        + " Key: " + (key == null ? "[null]" : Strings.obfuscate(key)));
                continue;
            }

            // filter out context attrs that should not be included
            if (logAttrKey.type == LogAttributeType.CONTEXT && !contextDataKeyFilter.shouldInclude(logAttrKey.getKey())) {
                continue;
            }

            String prefixedKey = mapInternString(logAttrKey.getPrefixedKey());
            if (value instanceof String) {
                sender.addAttribute(prefixedKey, mapInternString((String) value), METHOD);
            } else if (value instanceof Number) {
                sender.addAttribute(prefixedKey, (Number) value, METHOD);
            } else if (value instanceof Boolean) {
                sender.addAttribute(prefixedKey, (Boolean) value, METHOD);
            } else {
                // Java Agent specific - toString the value. This allows for e.g. enums as arguments.
                sender.addAttribute(prefixedKey, mapInternString(value.toString()), METHOD);
            }
        }
        return event;
    }

    /**
     * Validate attributes and add them to LogEvents
     */
    private static class LogEventAttributeSender extends AttributeSender {

        private static final String ATTRIBUTE_TYPE = "log";

        private final Map<String, Object> logEventAttributes;

        public LogEventAttributeSender(Map<String, Object> logEventAttributes) {
            super(new LogAttributeValidator(ATTRIBUTE_TYPE));
            this.logEventAttributes = logEventAttributes;
            setTransactional(false);
        }

        @Override
        protected String getAttributeType() {
            return ATTRIBUTE_TYPE;
        }

        @Override
        protected Map<String, Object> getAttributeMap() {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isCustomParametersAllowed()) {
                return logEventAttributes;
            }
            return null;
        }
    }

    @Override
    public Logs getTransactionLogs(AgentConfig config) {
        return new TransactionLogs(config, contextDataKeyFilter);
    }

    /**
     * Used to record LogEvents on Transactions
     */
    public static final class TransactionLogs implements Logs {
        private final Queue<LogEvent> events;
        private final ExcludeIncludeFilter contextDataKeyFilter;

        TransactionLogs(AgentConfig config, ExcludeIncludeFilter contextDataKeyFilter) {
            int maxSamplesStored = config.getApplicationLoggingConfig().getMaxSamplesStored();
            events = maxSamplesStored == 0 ? NoOpQueue.getInstance() : new LinkedBlockingQueue<>(maxSamplesStored);
            this.contextDataKeyFilter = contextDataKeyFilter;
        }

        @Override
        public void recordLogEvent(Map<LogAttributeKey, ?> attributes) {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected due to high security mode being enabled.", LOG_EVENT_TYPE);
                return;
            }

            LogEvent event = createValidatedEvent(attributes, contextDataKeyFilter);
            if (events.offer(event)) {
                Agent.LOG.log(Level.FINEST, "Added event of type {0} in Transaction.", LOG_EVENT_TYPE);
            } else {
                // Too many events are cached on the transaction, send directly to the reservoir.
                String applicationName = ServiceFactory.getRPMService().getApplicationName();
                ServiceFactory.getServiceManager().getLogSenderService().storeEvent(applicationName, event);
            }
        }

        @VisibleForTesting
        public List<LogEvent> getEventsForTesting() {
            return new ArrayList<>(events);
        }
    }
}
