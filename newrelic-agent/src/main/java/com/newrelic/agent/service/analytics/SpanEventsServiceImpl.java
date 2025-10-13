/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Harvestable;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.HttpParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The {@link SpanEventsServiceImpl} collects span events and transmits them to the collectors.
 * <p>
 * Ideally, all span events are stored until harvest and then transmitted. If the number of events exceeds a
 * configurable limit, events are replaced using a "reservoir" priority sampling algorithm.
 * <p>
 * This service can be configured using {@code span_events} with {@code enabled} or {@code max_samples_stored}.
 */
public class SpanEventsServiceImpl extends AbstractService implements AgentConfigListener, SpanEventsService, TransactionListener {

    private final ReservoirManager<SpanEvent> reservoirManager;
    private final ReservoirManager.EventSender<SpanEvent> collectorSender;
    private final Consumer<SpanEvent> eventBackendStorage;
    private final SpanEventCreationDecider spanEventCreationDecider;
    private final List<Harvestable> harvestables = new ArrayList<>();
    private final TracerToSpanEvent tracerToSpanEvent;

    private volatile SpanEventsConfig spanEventsConfig;

    public SpanEventsServiceImpl(Builder builder) {
        super(SpanEventsServiceImpl.class.getSimpleName());
        this.reservoirManager = builder.reservoirManager;
        this.collectorSender = builder.collectorSender;
        this.eventBackendStorage = builder.eventBackendStorage;
        this.spanEventCreationDecider = builder.spanEventCreationDecider;
        this.tracerToSpanEvent = builder.tracerToSpanEvent;
        this.spanEventsConfig = builder.spanEventsConfig;
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        // If this transaction is sampled and span events are enabled we should generate all of the transaction segment events
        if (isSpanEventsEnabled() && spanEventCreationDecider.shouldCreateSpans(transactionData)) {
            // This is where all Transaction Segment Spans gets created. To only send specific types of Span Events, handle that here.
            Tracer rootTracer = transactionData.getRootTracer();
            storeSafely(transactionData, rootTracer, true, transactionStats);

            Collection<Tracer> tracers = transactionData.getTracers();
            for (Tracer tracer : tracers) {
                if (tracer.isTransactionSegment()) {
                    storeSafely(transactionData, tracer, false, transactionStats);
                }
            }
        }
    }

    private void storeSafely(TransactionData transactionData, Tracer rootTracer, boolean isRoot, TransactionStats transactionStats) {
        try {
            createAndStoreSpanEvent(rootTracer, transactionData, isRoot, transactionStats);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, t, "An error occurred creating span event for tracer: {0} in tx: {1}", rootTracer, transactionData);
        }
    }

    private void createAndStoreSpanEvent(Tracer tracer, TransactionData transactionData, boolean isRoot,
            TransactionStats transactionStats) {
        boolean crossProcessOnly = spanEventsConfig.isCrossProcessOnly();
        if (crossProcessOnly && !isCrossProcessTracer(tracer)) {
            // We are in "cross_process_only" mode and we have a non datastore/external tracer. Return before we create anything.
            return;
        }

        String appName = transactionData.getApplicationName();
        SamplingPriorityQueue<SpanEvent> reservoir = getOrCreateDistributedSamplingReservoir(appName);

        SpanEvent spanEvent = tracerToSpanEvent.createSpanEvent(tracer, transactionData, transactionStats, isRoot, crossProcessOnly);
        storeEvent(spanEvent);
    }

    private boolean isCrossProcessTracer(Tracer tracer) {
        return tracer.getExternalParameters() instanceof HttpParameters || tracer.getExternalParameters() instanceof DatastoreParameters;
    }

    @Override
    public void harvestEvents(final String appName) {
        if (!spanEventsConfig.isEnabled() || reservoirManager.getMaxSamplesStored() <= 0) {
            clearReservoir();
            return;
        }
        long startTimeInNanos = System.nanoTime();
        final ReservoirManager.HarvestResult result = reservoirManager.attemptToSendReservoir(appName, collectorSender, logger);
        if (result != null) {
            final long durationInNanos = System.nanoTime() - startTimeInNanos;
            ServiceFactory.getStatsService().doStatsWork(new StatsWork() {
                @Override
                public void doWork(StatsEngine statsEngine) {
                    recordSupportabilityMetrics(statsEngine, durationInNanos, result.sent, result.seen);
                }

                @Override
                public String getAppName() {
                    return appName;
                }
            },  "HarvestResult");
        }
    }

    private void recordSupportabilityMetrics(StatsEngine statsEngine, long durationInNanos, int numberOfEventsSent, int numberOfEventsSeen) {
        statsEngine.getStats(MetricNames.SUPPORTABILITY_SPAN_EVENT_TOTAL_EVENTS_SENT)
                .incrementCallCount(numberOfEventsSent);
        statsEngine.getStats(MetricNames.SUPPORTABILITY_SPAN_EVENT_TOTAL_EVENTS_SEEN)
                .incrementCallCount(numberOfEventsSeen);
        statsEngine.getStats(MetricNames.SUPPORTABILITY_SPAN_EVENT_TOTAL_EVENTS_DISCARDED)
                .incrementCallCount(numberOfEventsSeen - numberOfEventsSent);
        statsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_SPAN_SERVICE_EVENT_HARVEST_TRANSMIT)
                .recordResponseTime(durationInNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public String getEventHarvestIntervalMetric() {
        return MetricNames.SUPPORTABILITY_SPAN_SERVICE_EVENT_HARVEST_INTERVAL;
    }

    @Override
    public String getReportPeriodInSecondsMetric() {
        return MetricNames.SUPPORTABILITY_SPAN_EVENT_SERVICE_REPORT_PERIOD_IN_SECONDS;
    }

    @Override
    public String getEventHarvestLimitMetric() {
        return MetricNames.SUPPORTABILITY_SPAN_EVENT_DATA_HARVEST_LIMIT;
    }


    @Override
    public boolean isEnabled() {
        return spanEventsConfig.isEnabled();
    }

    private boolean isSpanEventsEnabled() {
        return spanEventsConfig.isEnabled() && reservoirManager.getMaxSamplesStored() > 0;
    }

    @Override
    protected void doStart() throws Exception {
        if (isEnabled()) {
            // track feature for angler
            StatsService statsService = ServiceFactory.getServiceManager().getStatsService();
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_SPAN_EVENTS);
        }
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        removeHarvestables();
        clearReservoir();
    }

    private void removeHarvestables() {
        for (Harvestable harvestable : harvestables) {
            ServiceFactory.getHarvestService().removeHarvestable(harvestable);
        }
    }

    @Override
    public void storeEvent(SpanEvent event) {
        eventBackendStorage.accept(event);
    }

    @Override
    public int getMaxSamplesStored() {
        return reservoirManager.getMaxSamplesStored();
    }

    /**
     * For the setting to take effect, the reservoir must be re-created. Callers should harvest pending events before calling this.
     */
    @Override
    public void setMaxSamplesStored(int maxSamplesStored) {
        reservoirManager.setMaxSamplesStored(maxSamplesStored);
    }

    @Override
    public void clearReservoir() {
        reservoirManager.clearReservoir();
    }

    @Override
    public void addHarvestableToService(String appName) {
            Harvestable harvestable = new SpanEventHarvestableImpl(this, appName);
            ServiceFactory.getHarvestService().addHarvestable(harvestable);
            harvestables.add(harvestable);
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        boolean wasEnabled = isEnabled();
        spanEventsConfig = agentConfig.getSpanEventsConfig();

        if (!wasEnabled && isEnabled()) {
            // track feature for angler
            StatsService statsService = ServiceFactory.getServiceManager().getStatsService();
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_SPAN_EVENTS);
        }
    }

    @Override
    public SamplingPriorityQueue<SpanEvent> getOrCreateDistributedSamplingReservoir(String appName) {
        return reservoirManager.getOrCreateReservoir(appName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AgentConfig agentConfig;
        private ReservoirManager<SpanEvent> reservoirManager;
        private ReservoirManager.EventSender<SpanEvent> collectorSender;
        private Consumer<SpanEvent> eventBackendStorage;
        private SpanEventCreationDecider spanEventCreationDecider;
        private SpanEventsConfig spanEventsConfig;
        private TracerToSpanEvent tracerToSpanEvent;

        public Builder agentConfig(AgentConfig agentConfig) {
            this.agentConfig = agentConfig;
            return this;
        }

        public Builder reservoirManager(ReservoirManager<SpanEvent> reservoirManager) {
            this.reservoirManager = reservoirManager;
            return this;
        }

        public Builder collectorSender(ReservoirManager.EventSender<SpanEvent> collectorSender) {
            this.collectorSender = collectorSender;
            return this;
        }

        public Builder eventBackendStorage(Consumer<SpanEvent> eventBackendStorage) {
            this.eventBackendStorage = eventBackendStorage;
            return this;
        }

        public Builder spanEventCreationDecider(SpanEventCreationDecider decider) {
            this.spanEventCreationDecider = decider;
            return this;
        }

        public Builder spanEventsConfig(SpanEventsConfig spanEventsConfig) {
            this.spanEventsConfig = spanEventsConfig;
            return this;
        }

        public Builder tracerToSpanEvent(TracerToSpanEvent tracerToSpanEvent) {
            this.tracerToSpanEvent = tracerToSpanEvent;
            return this;
        }

        public SpanEventsServiceImpl build() {
            if (spanEventsConfig == null) {
                spanEventsConfig = agentConfig.getSpanEventsConfig();
            }
            return new SpanEventsServiceImpl(this);
        }
    }
}
