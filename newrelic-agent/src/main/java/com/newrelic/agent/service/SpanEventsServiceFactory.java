/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.errors.ErrorAnalyzerImpl;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.CollectorSpanEventReservoirManager;
import com.newrelic.agent.service.analytics.CollectorSpanEventSender;
import com.newrelic.agent.service.analytics.InfiniteTracingEnabledCheck;
import com.newrelic.agent.service.analytics.SpanErrorBuilder;
import com.newrelic.agent.service.analytics.SpanEventCreationDecider;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TracerToSpanEvent;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Responsible for creating a preconfigured instance of SpanEventsService
 */
public class SpanEventsServiceFactory {

    private final ConfigService configService;
    private final RPMServiceManager rpmServiceManager;
    private final InfiniteTracingEnabledCheck infiniteTracingEnabledCheck;
    private final TransactionService transactionService;
    private final ReservoirManager<SpanEvent> reservoirManager;
    private final Consumer<SpanEvent> infiniteTracingConsumer;
    private final SpanEventCreationDecider spanEventCreationDecider;
    private final EnvironmentService environmentService;
    private final TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics;

    private SpanEventsServiceFactory(Builder builder) {
        this.configService = builder.configService;
        this.rpmServiceManager = builder.rpmServiceManager;
        this.infiniteTracingConsumer = builder.infiniteTracingConsumer;
        this.infiniteTracingEnabledCheck = builder.infiniteTracingEnabledCheck;
        this.transactionService = builder.transactionService;
        this.reservoirManager = builder.reservoirManager;
        this.spanEventCreationDecider = builder.spanEventCreationDecider;
        this.environmentService = builder.environmentService;
        this.transactionDataToDistributedTraceIntrinsics = builder.transactionDataToDistributedTraceIntrinsics;
    }

    public SpanEventsService build() {
        ReservoirManager.EventSender<SpanEvent> collectorSpanEventSender = new CollectorSpanEventSender(rpmServiceManager);

        AgentConfig agentConfig = configService.getDefaultAgentConfig();

        Consumer<SpanEvent> eventStorageBackend = buildStorageBackendConsumer(reservoirManager);

        SpanErrorBuilder defaultSpanErrorBuilder = buildDefaultSpanErrorBuilder(agentConfig);

        Map<String, SpanErrorBuilder> errorBuilderForApp = buildSpanEventErrorBuilder(agentConfig, defaultSpanErrorBuilder);

        configureUpdateOnConfigChange(errorBuilderForApp);

        TracerToSpanEvent tracerToSpanEvent = new TracerToSpanEvent(errorBuilderForApp, environmentService, transactionDataToDistributedTraceIntrinsics, defaultSpanErrorBuilder);

        SpanEventsServiceImpl result = SpanEventsServiceImpl.builder()
                .agentConfig(agentConfig)
                .reservoirManager(reservoirManager)
                .collectorSender(collectorSpanEventSender)
                .eventBackendStorage(eventStorageBackend)
                .spanEventCreationDecider(spanEventCreationDecider)
                .tracerToSpanEvent(tracerToSpanEvent)
                .build();

        configService.addIAgentConfigListener(result);
        transactionService.addTransactionListener(result);

        return result;
    }

    private void configureUpdateOnConfigChange(final Map<String, SpanErrorBuilder> errorBuilderForApp) {
        configService.addIAgentConfigListener((appName, agentConfig) ->
                errorBuilderForApp.put(agentConfig.getApplicationName(), new SpanErrorBuilder(
                        new ErrorAnalyzerImpl(agentConfig.getErrorCollectorConfig()),
                        new ErrorMessageReplacer(agentConfig.getStripExceptionConfig())
                ))
        );
    }

    private Map<String, SpanErrorBuilder> buildSpanEventErrorBuilder(AgentConfig agentConfig, SpanErrorBuilder spanErrorBuilder) {
        Map<String, SpanErrorBuilder> result = new HashMap<>();
        result.put(agentConfig.getApplicationName(), spanErrorBuilder);
        return result;
    }

    private SpanErrorBuilder buildDefaultSpanErrorBuilder(AgentConfig agentConfig) {
        return new SpanErrorBuilder(
                new ErrorAnalyzerImpl(agentConfig.getErrorCollectorConfig()),
                new ErrorMessageReplacer(agentConfig.getStripExceptionConfig()));
    }

    private Consumer<SpanEvent> buildStorageBackendConsumer(final ReservoirManager<SpanEvent> reservoirManager) {
        if (infiniteTracingEnabledCheck.isEnabled()) {
            return infiniteTracingConsumer;
        }
        return new ReservoirAddingSpanEventConsumer(reservoirManager, configService);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ConfigService configService;
        private RPMServiceManager rpmServiceManager;
        private Consumer<SpanEvent> infiniteTracingConsumer;
        private InfiniteTracingEnabledCheck infiniteTracingEnabledCheck;
        private TransactionService transactionService;
        private ReservoirManager<SpanEvent> reservoirManager;
        private SpanEventCreationDecider spanEventCreationDecider;
        private EnvironmentService environmentService;
        private TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics;

        public Builder configService(ConfigService configService) {
            this.configService = configService;
            return this;
        }

        public Builder rpmServiceManager(RPMServiceManager rpmServiceManager) {
            this.rpmServiceManager = rpmServiceManager;
            return this;
        }

        public Builder infiniteTracingConsumer(Consumer<SpanEvent> infiniteTracingConsumer) {
            this.infiniteTracingConsumer = infiniteTracingConsumer;
            return this;
        }

        public Builder transactionService(TransactionService transactionService) {
            this.transactionService = transactionService;
            return this;
        }

        public Builder reservoirManager(ReservoirManager<SpanEvent> reservoirManager) {
            this.reservoirManager = reservoirManager;
            return this;
        }

        public Builder spanEventCreationDecider(SpanEventCreationDecider spanEventCreationDecider) {
            this.spanEventCreationDecider = spanEventCreationDecider;
            return this;
        }

        public Builder environmentService(EnvironmentService environmentService) {
            this.environmentService = environmentService;
            return this;
        }

        public Builder transactionDataToDistributedTraceIntrinsics(TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics) {
            this.transactionDataToDistributedTraceIntrinsics = transactionDataToDistributedTraceIntrinsics;
            return this;
        }

        public SpanEventsService build() {
            infiniteTracingEnabledCheck = infiniteTracingEnabledCheck == null ?
                    new InfiniteTracingEnabledCheck(configService) : infiniteTracingEnabledCheck;
            reservoirManager = reservoirManager == null ?
                    new CollectorSpanEventReservoirManager(configService) : reservoirManager;
            return new SpanEventsServiceFactory(this).build();
        }
    }
}
