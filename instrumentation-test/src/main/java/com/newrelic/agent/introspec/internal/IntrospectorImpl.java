/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionStatsListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.introspec.DataStoreRequest;
import com.newrelic.agent.introspec.Error;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Map;

class IntrospectorImpl implements Introspector, ExtendedTransactionListener, TransactionStatsListener {

    private IntrospectTxData data;

    private IntrospectorImpl() {
        data = new IntrospectTxData();
    }

    public static IntrospectorImpl createIntrospector(Map<String, Object> config) {
        initialize(config);
        IntrospectorImpl impl = new IntrospectorImpl();
        ServiceFactory.getTransactionService().addTransactionListener(impl);
        ServiceFactory.getTransactionService().addTransactionStatsListener(impl);
        return impl;
    }

    private static IntrospectorServiceManager initialize(Map<String, Object> config) {
        IntrospectorServiceManager manager = IntrospectorServiceManager.createAndInitialize(config);
        try {
            manager.start();
        } catch (Exception e) {
            // app will not work correctly
        }

        // initialize services / APIs
        com.newrelic.api.agent.NewRelicApiImplementation.initialize();
        com.newrelic.agent.PrivateApiImpl.initialize(Agent.LOG);
        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        com.newrelic.agent.cloud.CloudApiImpl.initialize();
        return manager;
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        data.addStartedTransaction(transaction);
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
    }

    @Override
    public void dispatcherTransactionStatsFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data.addFinishedTransaction(transactionData, transactionStats);
    }

    @Override
    public void clear() {
        data.clear();
        IntrospectorInsightsService customEventService = (IntrospectorInsightsService) ServiceFactory.getServiceManager().getInsights();
        customEventService.clear();
        IntrospectorErrorService errorService = (IntrospectorErrorService) ServiceFactory.getRPMService().getErrorService();
        errorService.clear();
        IntrospectorStatsService statsService = (IntrospectorStatsService) ServiceFactory.getStatsService();
        statsService.clear();
        IntrospectorTransactionTraceService traceService = (IntrospectorTransactionTraceService) ServiceFactory.getTransactionTraceService();
        traceService.clear();
        clearSpanEvents();
    }

    public void cleanup() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    @Override
    public Collection<String> getTransactionNames() {
        return data.getTransactionNames();
    }

    @Override
    public Map<String, TracedMetricData> getMetricsForTransaction(String transaction) {
        IntrospectorStatsService service = (IntrospectorStatsService) ServiceFactory.getStatsService();
        return service.getScopedMetrics(transaction);
    }

    @Override
    public Map<String, TracedMetricData> getUnscopedMetrics() {
        IntrospectorStatsService service = (IntrospectorStatsService) ServiceFactory.getStatsService();
        return service.getUnscopedMetrics();
    }

    @Override
    public Collection<Event> getCustomEvents(String type) {
        IntrospectorInsightsService service = (IntrospectorInsightsService) ServiceFactory.getServiceManager().getInsights();
        return service.getEvents(type);
    }

    @Override
    public Collection<String> getCustomEventTypes() {
        IntrospectorInsightsService service = (IntrospectorInsightsService) ServiceFactory.getServiceManager().getInsights();
        return service.getEventTypes();
    }

    @Override
    public Collection<ExternalRequest> getExternalRequests(String transaction) {
        return data.getExternals(transaction);
    }

    @Override
    public Collection<DataStoreRequest> getDataStores(String transaction) {
        return data.getDatastores(transaction);
    }

    @Override
    public Collection<Error> getErrors() {
        IntrospectorErrorService service = (IntrospectorErrorService) ServiceFactory.getRPMService().getErrorService();
        return service.getAllErrors();
    }

    @Override
    public Collection<Error> getErrorsForTransaction(String transactionName) {
        IntrospectorErrorService service = (IntrospectorErrorService) ServiceFactory.getRPMService().getErrorService();
        return service.getErrors(transactionName);
    }

    @Override
    public Collection<com.newrelic.agent.introspec.TransactionEvent> getTransactionEvents(String transactionName) {
        return data.getTransactionEvents(transactionName);
    }

    @Override
    public Collection<com.newrelic.agent.introspec.ErrorEvent> getErrorEvents() {
        IntrospectorErrorService service = (IntrospectorErrorService) ServiceFactory.getRPMService().getErrorService();
        return service.getErrorEvents();
    }

    @Override
    public Collection<com.newrelic.agent.introspec.ErrorEvent> getErrorEventsForTransaction(String transactionName) {
        IntrospectorErrorService service = (IntrospectorErrorService) ServiceFactory.getRPMService().getErrorService();
        return service.getErrorEventsForTransaction(transactionName);
    }

    public Collection<TransactionTrace> getTransactionTracesForTransaction(String transactionName) {
        IntrospectorTransactionTraceService traceService = (IntrospectorTransactionTraceService) ServiceFactory.getTransactionTraceService();
        return traceService.getTracesForTransaction(transactionName);
    }

    @Override
    public int getFinishedTransactionCount() {
        // Default timeout is 5 seconds
        return getFinishedTransactionCount(5000);
    }

    @Override
    public int getFinishedTransactionCount(long timeoutMS) {
        long endTime = System.currentTimeMillis() + timeoutMS;
        while (data.getUnfinishedTxCount() > 0 && System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return data.getTxCount();
    }

    @Override
    public Integer getServerPort() {
        return ServiceFactory.getServiceManager()
                .getEnvironmentService()
                .getEnvironment()
                .getAgentIdentity()
                .getServerPort();
    }

    @Override
    public String getDispatcher() {
        return ServiceFactory.getServiceManager()
                .getEnvironmentService()
                .getEnvironment()
                .getAgentIdentity()
                .getDispatcher();
    }

    @Override
    public String getDispatcherVersion() {
        return ServiceFactory.getServiceManager()
                .getEnvironmentService()
                .getEnvironment()
                .getAgentIdentity()
                .getDispatcherVersion();
    }

    @Override
    public Collection<SpanEvent> getSpanEvents() {
       IntrospectorSpanEventService service = (IntrospectorSpanEventService) ServiceFactory.getServiceManager().getSpanEventsService();
       return service.getSpanEvents();
    }

    @Override
    public void clearSpanEvents() {
        IntrospectorSpanEventService service = (IntrospectorSpanEventService) ServiceFactory.getServiceManager().getSpanEventsService();
        service.clearReservoir();
    }

    @Override
    public Collection<LogEvent> getLogEvents() {
       IntrospectorLogSenderService service = (IntrospectorLogSenderService) ServiceFactory.getServiceManager().getLogSenderService();
       return service.getLogEvents();
    }

    @Override
    public void clearLogEvents() {
        IntrospectorLogSenderService service = (IntrospectorLogSenderService) ServiceFactory.getServiceManager().getLogSenderService();
        service.clearReservoir();
    }

    @Override
    public int getRandomPort() {
        int port;
        try {
            ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to allocate ephemeral port");
        }
        return port;
    }
}
