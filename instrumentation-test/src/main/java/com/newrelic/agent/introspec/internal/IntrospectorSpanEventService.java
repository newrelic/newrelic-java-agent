/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.errors.ErrorAnalyzerImpl;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.SpanErrorBuilder;
import com.newrelic.agent.service.analytics.SpanEventCreationDecider;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TracerToSpanEvent;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.stats.TransactionStats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IntrospectorSpanEventService extends SpanEventsServiceImpl {
    Queue<com.newrelic.agent.introspec.SpanEvent> events = new ConcurrentLinkedQueue<>();

    public IntrospectorSpanEventService(AgentConfig agentConfig, ReservoirManager<SpanEvent> reservoirManager,
                                        ReservoirManager.EventSender<SpanEvent> collectorSender, Consumer<SpanEvent> eventStorageBackend,
                                        SpanEventCreationDecider spanEventCreationDecider, EnvironmentService environmentService,
            TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics) {
        super(SpanEventsServiceImpl.builder()
                .agentConfig(agentConfig)
                .reservoirManager(reservoirManager)
                .collectorSender(collectorSender)
                .eventBackendStorage(eventStorageBackend)
                .spanEventCreationDecider(spanEventCreationDecider)
                .spanEventsConfig(agentConfig.getSpanEventsConfig())
                .tracerToSpanEvent(buildTracerToSpanEvent(agentConfig, environmentService, transactionDataToDistributedTraceIntrinsics)));
    }

    private static TracerToSpanEvent buildTracerToSpanEvent(AgentConfig agentConfig, EnvironmentService environmentService,
            TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics) {
        Map<String, SpanErrorBuilder> map = new HashMap<>();
        SpanErrorBuilder spanErrorBuilder = new SpanErrorBuilder(
                new ErrorAnalyzerImpl(agentConfig.getErrorCollectorConfig()),
                new ErrorMessageReplacer(agentConfig.getStripExceptionConfig()));
        map.put(agentConfig.getApplicationName(), spanErrorBuilder);
        return new TracerToSpanEvent(map, environmentService, transactionDataToDistributedTraceIntrinsics, spanErrorBuilder);
    }

    @Override
    public void storeEvent(SpanEvent spanEvent) {
        events.add(new SpanEventImpl(spanEvent));
    }

    @Override
    public void addHarvestableToService(String s) {
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        super.dispatcherTransactionFinished(transactionData, transactionStats);
    }

    @Override
    public DistributedSamplingPriorityQueue<SpanEvent> getOrCreateDistributedSamplingReservoir(String appName) {
        return new DistributedSamplingPriorityQueue<>(10);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public IAgentLogger getLogger() {
        return null;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isStartedOrStarting() {
        return false;
    }

    @Override
    public boolean isStoppedOrStopping() {
        return false;
    }

    @Override
    public void clearReservoir() {
        events.clear();
    }

    public Collection<com.newrelic.agent.introspec.SpanEvent> getSpanEvents() {
        return events;
    }
}
