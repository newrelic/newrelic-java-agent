/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.Agent;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.Harvestable;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.attributes.CustomEventAttributeValidator;
import com.newrelic.agent.attributes.LlmEventAttributeValidator;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.LlmCustomInsightsEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.Insights;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class InsightsServiceImpl extends AbstractService implements InsightsService {
    // Whether the service as a whole is enabled. Disabling shuts down all analytic events for transactions.
    private volatile boolean enabled;
    // Key is the app name, value is if it is enabled - should be a limited number of names
    private final ConcurrentMap<String, Boolean> isEnabledForApp = new ConcurrentHashMap<>();
    /*
     * Number of events in the reservoir sampling buffer per-app. All apps get the same value. Synthetics are buffered
     * separately per app using a deterministic algorithm.
     */
    private volatile int maxSamplesStored;

    // Key is app name, value is collection of per-transaction analytic events for next harvest for that app.
    private final ConcurrentHashMap<String, DistributedSamplingPriorityQueue<CustomInsightsEvent>> reservoirForApp = new ConcurrentHashMap<>();

    private static final LoadingCache<String, String> stringCache = Caffeine.newBuilder().maximumSize(1000)
            .expireAfterAccess(70, TimeUnit.SECONDS).executor(Runnable::run).build(key -> key);

    protected final ExtendedTransactionListener transactionListener = new ExtendedTransactionListener() {

        @Override
        public void dispatcherTransactionStarted(Transaction transaction) {
        }

        @Override
        public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
            TransactionInsights data = (TransactionInsights) transactionData.getInsightsData();
            storeEvents(transactionData.getApplicationName(), transactionData.getPriority(), data.events);
        }

        @Override
        public void dispatcherTransactionCancelled(Transaction transaction) {
            // Even if the transaction is cancelled we still want to send up any events that were held in it
            TransactionInsights data = (TransactionInsights) transaction.getInsightsData();
            storeEvents(transaction.getApplicationName(), transaction.getPriority(), data.events);
        }

    };

    protected final AgentConfigListener configListener = new AgentConfigListener() {
        @Override
        public void configChanged(String appName, AgentConfig agentConfig) {
            // if the config has changed for the app, just remove it and regenerate enabled next transaction
            isEnabledForApp.remove(appName);
            enabled = agentConfig.getInsightsConfig().isEnabled();
        }
    };

    private List<Harvestable> harvestables = new ArrayList<>();

    public InsightsServiceImpl() {
        super("Insights");
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        maxSamplesStored = config.getInsightsConfig().getMaxSamplesStored();
        enabled = config.getInsightsConfig().isEnabled();
        isEnabledForApp.put(config.getApplicationName(), enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getTransactionService().addTransactionListener(transactionListener);
        ServiceFactory.getConfigService().addIAgentConfigListener(configListener);
    }

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

    @Override
    public void recordCustomEvent(String eventType, Map<String, ?> attributes) {
        if (customEventsDisabled(eventType)) {
            return;
        }

        if (AnalyticsEvent.isValidType(eventType)) {
            Transaction transaction = ServiceFactory.getTransactionService().getTransaction(false);
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
                storeEvent(applicationName, eventType, attributes);
            } else {
                transaction.getInsightsData().recordCustomEvent(eventType, attributes);
            }
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_RECORD_CUSTOM_EVENT);
        } else {
            Agent.LOG.log(Level.WARNING, "Custom event with invalid type of {0} was reported but ignored."
                    + " Event types must match /^[a-zA-Z0-9:_ ]+$/, be non-null, and less than 256 chars.", eventType);
        }
    }

    private void storeEvents(String appName, float priority, Collection<CustomInsightsEvent> events) {
        if (events.size() > 0) {
            DistributedSamplingPriorityQueue<CustomInsightsEvent> eventList = getReservoir(appName);
            for (CustomInsightsEvent event : events) {
                // Set "priority" on CustomEvents based on priority value from Transaction
                event.setPriority(priority);
                eventList.add(event);
            }
        }
    }

    public void addHarvestableToService(String appName) {
        Harvestable harvestable = new InsightsHarvestableImpl(this, appName);
        ServiceFactory.getHarvestService().addHarvestable(harvestable);
        harvestables.add(harvestable);
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public void setMaxSamplesStored(int maxSamplesStored) {
        this.maxSamplesStored = maxSamplesStored;
    }

    public void clearReservoir() {
        reservoirForApp.clear();
    }

    public void clearReservoir(String appName) {
        DistributedSamplingPriorityQueue<CustomInsightsEvent> reservoir = reservoirForApp.get(appName);
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

    @Override
    public void storeEvent(String appName, CustomInsightsEvent event) {
        if (customEventsDisabled(event.getType())) {
            return;
        }

        DistributedSamplingPriorityQueue<CustomInsightsEvent> eventList = getReservoir(appName);
        eventList.add(event);
        Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0}", event.getType()));
    }

    private void storeEvent(String appName, String eventType, Map<String, ?> attributes) {
        if (customEventsDisabled(eventType)) {
            return;
        }

        DistributedSamplingPriorityQueue<CustomInsightsEvent> eventList = getReservoir(appName);
        eventList.add(createValidatedEvent(eventType, attributes));
        Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0}", eventType));
    }

    private boolean customEventsDisabled(String eventType) {
        if (!enabled) {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected due to high security mode being enabled.", eventType);
            } else {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected. custom_insights_events not enabled.", eventType);
            }

            return true; // Custom events are disabled
        }

        return false; // Custom events are enabled
    }

    @VisibleForTesting
    public DistributedSamplingPriorityQueue<CustomInsightsEvent> getReservoir(String appName) {
        DistributedSamplingPriorityQueue<CustomInsightsEvent> result = reservoirForApp.get(appName);
        while (result == null) {
            // I don't think this loop can actually execute more than once, but it's prudent to assume it can.
            reservoirForApp.putIfAbsent(appName, new DistributedSamplingPriorityQueue<CustomInsightsEvent>(appName, "Insights Service", maxSamplesStored));
            result = reservoirForApp.get(appName);
        }
        return result;
    }

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

        final DistributedSamplingPriorityQueue<CustomInsightsEvent> reservoir = this.reservoirForApp.put(appName,
                new DistributedSamplingPriorityQueue<CustomInsightsEvent>(appName, "Insights Service", maxSamplesStored));

        if (reservoir != null && reservoir.size() > 0) {
            try {
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(appName)
                        .sendCustomAnalyticsEvents(maxSamplesStored, reservoir.getNumberOfTries(), Collections.unmodifiableList(reservoir.asList()));
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
                }, reservoir.getServiceName());

                if (reservoir.size() < reservoir.getNumberOfTries()) {
                    int dropped = reservoir.getNumberOfTries() - reservoir.size();
                    Agent.LOG.log(Level.FINE, "Dropped {0} custom events out of {1}.", dropped, reservoir.getNumberOfTries());
                }
            } catch (HttpError e) {
                if (!e.discardHarvestData()) {
                    Agent.LOG.log(Level.FINE, "Unable to send custom events. Unsent events will be included in the next harvest.", e);
                    // Save unsent data by merging it with current data using reservoir algorithm
                    DistributedSamplingPriorityQueue<CustomInsightsEvent> currentReservoir = reservoirForApp.get(appName);
                    currentReservoir.retryAll(reservoir);
                } else {
                    // discard harvest data
                    reservoir.clear();
                    Agent.LOG.log(Level.FINE, "Unable to send custom events. Unsent events will be dropped.", e);
                }
            } catch (Exception e) {
                // discard harvest data
                reservoir.clear();
                Agent.LOG.log(Level.FINE, "Unable to send custom events. Unsent events will be dropped.", e);
            }
        }
    }

    @Override
    public String getEventHarvestIntervalMetric() {
        return MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_EVENT_HARVEST_INTERVAL;
    }

    @Override
    public String getReportPeriodInSecondsMetric() {
        return MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_REPORT_PERIOD_IN_SECONDS;
    }

    @Override
    public String getEventHarvestLimitMetric() {
        return MetricNames.SUPPORTABILITY_CUSTOM_EVENT_DATA_HARVEST_LIMIT;
    }

    private void recordSupportabilityMetrics(StatsEngine statsEngine, long durationInNanoseconds,
            DistributedSamplingPriorityQueue<CustomInsightsEvent> reservoir) {
        statsEngine.getStats(MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_CUSTOMER_SENT)
                .incrementCallCount(reservoir.size());
        statsEngine.getStats(MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_CUSTOMER_SEEN)
                .incrementCallCount(reservoir.getNumberOfTries());
        statsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_EVENT_HARVEST_TRANSMIT)
                .recordResponseTime(durationInNanoseconds, TimeUnit.NANOSECONDS);
    }

    private boolean getIsEnabledForApp(AgentConfig config, String currentAppName) {
        Boolean appEnabled = currentAppName == null ? null : isEnabledForApp.get(currentAppName);
        if (appEnabled == null) {
            appEnabled = config.getInsightsConfig().isEnabled();
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

    private static CustomInsightsEvent createValidatedEvent(String eventType, Map<String, ?> attributes) {
        Map<String, Object> userAttributes = new HashMap<>(attributes.size());
        CustomInsightsEvent event = new CustomInsightsEvent(mapInternString(eventType), System.currentTimeMillis(), userAttributes,
                DistributedTraceServiceImpl.nextTruncatedFloat());

        // Now add the attributes from the argument map to the event using an AttributeSender.
        // An AttributeSender is the way to reuse all the existing attribute validations. We
        // also locally "intern" Strings because we anticipate a lot of reuse of the keys and,
        // possibly, the values. But there's an interaction: if the key or value is chopped
        // within the attribute sender, the modified value won't be "interned" in our map.
        AttributeSender sender;
        final String method;

        // CustomInsightsEvents are being overloaded to support some internal event types being sent to the same agent endpoint
        if (LlmCustomInsightsEvent.isLlmEvent(eventType)) {
            sender = new LlmEventAttributeSender(userAttributes);
            method = "add llm event attribute";
        } else {
            sender = new CustomEventAttributeSender(userAttributes);
            method = "add custom event attribute";
        }

        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // key or value is null, skip it with a log message and iterate to next entry in attributes.entrySet()
            if (key == null || value == null) {
                Agent.LOG.log(Level.FINEST, "Custom event [{0}] with invalid attributes key or value of null was reported for a transaction but ignored."
                                + " Each key should be a String and each value should be a String, Number, or Boolean. Key: {1} / Value: {2}",
                        eventType, (key == null ? "[null]" : Strings.obfuscate(key)));
                continue;
            }

            key = mapInternString(key);

            if (value instanceof String) {
                sender.addAttribute(key, mapInternString((String) value), method);
            } else if (value instanceof Number) {
                sender.addAttribute(key, (Number) value, method);
            } else if (value instanceof Boolean) {
                sender.addAttribute(key, (Boolean) value, method);
            } else {
                // Java Agent specific - toString the value. This allows for e.g. enums as arguments.
                sender.addAttribute(key, mapInternString(value.toString()), method);
            }
        }

        return event;
    }

    private static class CustomEventAttributeSender extends AttributeSender {

        private static final String ATTRIBUTE_TYPE = "custom";

        private final Map<String, Object> userAttributes;

        public CustomEventAttributeSender(Map<String, Object> userAttributes) {
            super(new CustomEventAttributeValidator(ATTRIBUTE_TYPE));
            this.userAttributes = userAttributes;
            setTransactional(false);
        }

        @Override
        protected String getAttributeType() {
            return ATTRIBUTE_TYPE;
        }

        @Override
        protected Map<String, Object> getAttributeMap() {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isCustomParametersAllowed()) {
                return userAttributes;
            }
            return null;
        }
    }

    /**
     * LlmEvent attribute validation rules differ from those of a standard CustomInsightsEvent
     */
    private static class LlmEventAttributeSender extends AttributeSender {
        private static final String ATTRIBUTE_TYPE = "llm";

        private final Map<String, Object> userAttributes;

        public LlmEventAttributeSender(Map<String, Object> userAttributes) {
            super(new LlmEventAttributeValidator(ATTRIBUTE_TYPE));
            this.userAttributes = userAttributes;
            // This will have the effect of only copying attributes onto LlmEvents if there is an active transaction
            setTransactional(true);
        }

        @Override
        protected String getAttributeType() {
            return ATTRIBUTE_TYPE;
        }

        @Override
        protected Map<String, Object> getAttributeMap() {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isCustomParametersAllowed()) {
                return userAttributes;
            }
            return null;
        }
    }

    @Override
    public Insights getTransactionInsights(AgentConfig config) {
        return new TransactionInsights(config);
    }

    public static final class TransactionInsights implements Insights {
        final LinkedBlockingQueue<CustomInsightsEvent> events;

        TransactionInsights(AgentConfig config) {
            int maxSamplesStored = config.getInsightsConfig().getMaxSamplesStored();
            events = new LinkedBlockingQueue<>(maxSamplesStored);
        }

        @Override
        public void recordCustomEvent(String eventType, Map<String, ?> attributes) {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected due to high security mode being enabled.", eventType);
                return;
            }

            if (AnalyticsEvent.isValidType(eventType)) {
                CustomInsightsEvent event = createValidatedEvent(eventType, attributes);
                if (events.offer(event)) {
                    Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0} in Transaction.", eventType));
                } else {
                    // Too many events are cached on the transaction, send directly to the reservoir.
                    String applicationName = ServiceFactory.getRPMService().getApplicationName();
                    ServiceFactory.getServiceManager().getInsights().storeEvent(applicationName, event);
                }
            } else {
                Agent.LOG.log(Level.WARNING, "Custom event with invalid type of {0} was reported for a transaction but ignored."
                        + " Event types must match /^[a-zA-Z0-9:_ ]+$/, be non-null, and less than 256 chars.", eventType);
            }
        }

        public List<CustomInsightsEvent> getEventsForTesting() {
            return new ArrayList<>(events);
        }
    }
}
