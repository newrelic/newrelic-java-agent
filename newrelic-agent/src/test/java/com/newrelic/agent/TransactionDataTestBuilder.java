/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.sql.SlowQueryListener;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.agent.transaction.TransactionTimer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionDataTestBuilder {
    private final String appName;
    private final AgentConfig agentConfig;
    private final Tracer tracer;

    private Transaction tx = mock(Transaction.class);
    private Dispatcher dispatcher = null;
    private Throwable throwable = null;
    private boolean expectedError = false;

    private String requestUri = "/uri";
    private String frontendMetricName = "/Frontend/UnitTest";

    private int responseStatus = 200;
    private String statusMessage = null;
    private SlowQueryListener slowQueryListener = null;
    private List<Tracer> tracers = null;
    private long startTime = System.currentTimeMillis();

    private Map<String, String> requestParams = Collections.emptyMap();
    private Map<String, Object> userParams = Collections.emptyMap();
    private Map<String, Object> agentParams = Collections.emptyMap();
    private Map<String, Object> errorParams = Collections.emptyMap();
    private Map<String, Object> intrinsics = Collections.emptyMap();
    private String synJobId = null;
    private String synMonitorId = null;
    private String synResourceId = null;
    private boolean includeDistributedTracePayload = false;
    private Multimap<Long, Duration> threadIdToDuration = ArrayListMultimap.create();

    private Transaction.PartialSampleType partialSampleType = null;

    public TransactionDataTestBuilder setTx(Transaction tx) {
        this.tx = tx;
        return this;
    }

    public TransactionDataTestBuilder setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    public TransactionDataTestBuilder setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    public TransactionDataTestBuilder setExpectedError(boolean expectedError) {
        this.expectedError = expectedError;
        return this;
    }

    public TransactionDataTestBuilder setRequestUri(String requestUri) {
        this.requestUri = requestUri;
        return this;
    }

    public TransactionDataTestBuilder setFrontendMetricName(String frontendMetricName) {
        this.frontendMetricName = frontendMetricName;
        return this;
    }

    public TransactionDataTestBuilder setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    public TransactionDataTestBuilder setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    public TransactionDataTestBuilder setSlowQueryListener(SlowQueryListener slowQueryListener) {
        this.slowQueryListener = slowQueryListener;
        return this;
    }

    public TransactionDataTestBuilder setTracers(List<Tracer> tracers) {
        this.tracers = tracers;
        return this;
    }

    public TransactionDataTestBuilder setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public TransactionDataTestBuilder setRequestParams(Map<String, String> requestParams) {
        this.requestParams = requestParams;
        return this;
    }

    public TransactionDataTestBuilder setUserParams(Map<String, Object> userParams) {
        this.userParams = userParams;
        return this;
    }

    public TransactionDataTestBuilder setAgentParams(Map<String, Object> agentParams) {
        this.agentParams = agentParams;
        return this;
    }

    public TransactionDataTestBuilder setErrorParams(Map<String, Object> errorParams) {
        this.errorParams = errorParams;
        return this;
    }

    public TransactionDataTestBuilder setIntrinsics(Map<String, Object> intrinsics) {
        this.intrinsics = intrinsics;
        return this;
    }

    public TransactionDataTestBuilder setSynJobId(String synJobId) {
        this.synJobId = synJobId;
        return this;
    }

    public TransactionDataTestBuilder setSynMonitorId(String synMonitorId) {
        this.synMonitorId = synMonitorId;
        return this;
    }

    public TransactionDataTestBuilder setSynResourceId(String synResourceId) {
        this.synResourceId = synResourceId;
        return this;
    }

    public TransactionDataTestBuilder setIncludeDistributedTracePayload(boolean includeDistributedTracePayload) {
        this.includeDistributedTracePayload = includeDistributedTracePayload;
        return this;
    }

    public TransactionDataTestBuilder setThreadIdToDuration(Multimap<Long, Duration> threadIdToDuration) {
        this.threadIdToDuration = threadIdToDuration;
        return this;
    }

    public TransactionDataTestBuilder setPartialSampleType(Transaction.PartialSampleType partialSampleType) {
        this.partialSampleType = partialSampleType;
        return this;
    }

    public TransactionDataTestBuilder(String appName, AgentConfig agentConfig, Tracer tracer) {
        this.appName = appName;
        this.agentConfig = agentConfig;
        this.tracer = tracer;
    }

    public TransactionData build() {
        when(tx.getRootTracer()).thenReturn(tracer);
        if (synJobId == null || synMonitorId == null || synResourceId == null) {
            when(tx.isSynthetic()).thenReturn(false);
        } else {
            when(tx.isSynthetic()).thenReturn(true);
        }

        when(tx.getGuid()).thenReturn("guid");

        Dispatcher mockDispatcher = mock(Dispatcher.class);
        when(mockDispatcher.getUri()).thenReturn(requestUri);
        when(mockDispatcher.isWebTransaction()).thenReturn(dispatcher == null ? true : dispatcher.isWebTransaction());
        when(tx.getDispatcher()).thenReturn(mockDispatcher);

        if (throwable != null) {
            when(tx.getThrowable()).thenReturn(new TransactionThrowable(throwable, expectedError, null));
        }

        PriorityTransactionName priorityTransactionName = mock(PriorityTransactionName.class);
        when(priorityTransactionName.getName()).thenReturn(frontendMetricName);
        when(tx.getPriorityTransactionName()).thenReturn(priorityTransactionName);

        Map<String, Map<String, String>> prefixed = new HashMap<>();
        prefixed.put("request.parameters.", requestParams);
        when(tx.getPrefixedAgentAttributes()).thenReturn(prefixed);
        when(tx.getUserAttributes()).thenReturn(userParams);
        when(tx.getAgentAttributes()).thenReturn(agentParams);
        when(tx.getErrorAttributes()).thenReturn(errorParams);
        when(tx.getIntrinsicAttributes()).thenReturn(intrinsics);

        when(tx.isIgnore()).thenReturn(false);
        when(tx.getStatus()).thenReturn(responseStatus);
        when(tx.getStatusMessage()).thenReturn(statusMessage);
        when(tx.isErrorReportableAndNotIgnored()).thenReturn(true);
        when(tx.getSpanProxy()).thenReturn(new SpanProxy());

        ErrorServiceImpl errorService = mock(ErrorServiceImpl.class);

        IRPMService rpmService = mock(IRPMService.class);
        when(rpmService.getApplicationName()).thenReturn(appName);
        when(rpmService.getErrorService()).thenReturn(errorService);
        when(tx.getRPMService()).thenReturn(rpmService);

        when(tx.getAgentConfig()).thenReturn(agentConfig);
        when(tx.getWallClockStartTimeMs()).thenReturn(startTime);
        if (slowQueryListener != null) {
            when(tx.getSlowQueryListener(anyBoolean())).thenReturn(slowQueryListener);
        }
        when(tx.getTracers()).thenReturn(tracers);

        CrossProcessTransactionState crossProcessTransactionState = mock(CrossProcessTransactionState.class);
        when(tx.getCrossProcessTransactionState()).thenReturn(crossProcessTransactionState);
        when(crossProcessTransactionState.getTripId()).thenReturn("tripId");

        InboundHeaderState ihs = mock(InboundHeaderState.class);
        when(ihs.getSyntheticsJobId()).thenReturn(synJobId);
        when(ihs.getSyntheticsMonitorId()).thenReturn(synMonitorId);
        when(ihs.getSyntheticsResourceId()).thenReturn(synResourceId);
        when(ihs.getSyntheticsVersion()).thenReturn(HeadersUtil.SYNTHETICS_MIN_VERSION);
        when(tx.getInboundHeaderState()).thenReturn(ihs);

        TransactionTimer timer = new TransactionTimer(tracer.getStartTime());
        timer.markTransactionActivityAsDone(tracer.getEndTime(), tracer.getDuration());
        timer.markTransactionAsDone();

        when(tx.getTransactionTimer()).thenReturn(timer);

        // when(tx.getApplicationName()).thenReturn(appName);

        Set<TransactionActivity> activities = new HashSet<>();
        for (Map.Entry<Long, Collection<Duration>> entry : threadIdToDuration.asMap().entrySet()) {
            for (Duration duration : entry.getValue()) {
                TransactionActivity activity = mock(TransactionActivity.class);
                when(activity.getThreadId()).thenReturn(entry.getKey());

                Tracer rootTracer = mock(Tracer.class);
                when(rootTracer.getStartTime()).thenReturn(duration.startTime);
                when(rootTracer.getEndTime()).thenReturn(duration.endTime);
                when(activity.getRootTracer()).thenReturn(rootTracer);
                activities.add(activity);
            }
        }
        when(tx.getFinishedChildren()).thenReturn(activities);

        if (includeDistributedTracePayload) {
            SpanProxy spanProxy = mock(SpanProxy.class);
            DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload("abc", "def", "def",
                    new Random().nextFloat());
            when(spanProxy.getInboundDistributedTracePayload()).thenReturn(payload);
            when(tx.getSpanProxy()).thenReturn(spanProxy);
        }

        when(tx.getPartialSampleType()).thenReturn(partialSampleType);

        return new TransactionData(tx, 0);
    }
}
