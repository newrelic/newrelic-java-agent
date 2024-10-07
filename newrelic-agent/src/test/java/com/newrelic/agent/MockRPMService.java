/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.superagent.HealthDataProducer;
import com.newrelic.agent.trace.TransactionTrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MockRPMService extends BaseRPMService {

    public final List<IProfile> profiles = Collections.synchronizedList(new ArrayList<IProfile>());
    public final List<com.newrelic.agent.profile.v2.IProfile> profilesV2 = Collections.synchronizedList(new ArrayList<com.newrelic.agent.profile.v2.IProfile>());
    private volatile boolean isConnected = false;
    private final CountDownLatch latch;
    private volatile int restartCount = 0;
    private final AtomicReference<String> appName = new AtomicReference<>();
    private volatile List<String> appNames;
    private boolean everConnected = false;
    private ErrorService errorService;
    private volatile ConnectionListener connectionListener;
    private volatile ConnectionConfigListener connectionConfigListener;
    private List<SqlTrace> sqlTraces;
    private List<TransactionTrace> traces = Collections.emptyList();
    private List<TracedError> errorTraces = new ArrayList<>();
    private final Collection<AnalyticsEvent> events = new ArrayList<>();
    private final AtomicInteger transactionEventsSeen = new AtomicInteger(0);
    private final AtomicInteger spanEventsSeen = new AtomicInteger(0);
    private final AtomicInteger customEventsSeen = new AtomicInteger(0);
    private final AtomicInteger logSenderEventsSeen = new AtomicInteger(0);
    private final AtomicInteger errorEventsSeen = new AtomicInteger(0);
    private final AtomicInteger errorTracesSeen = new AtomicInteger(0);
    private final AtomicInteger transactionTracesSeen = new AtomicInteger(0);
    private final MockHarvestListener mockHarvestListener;

    public interface MockHarvestListener {
        void mockHarvest(StatsEngine statsEngine);
    }

    public MockRPMService() {
        this(null, null);
    }

    public MockRPMService(CountDownLatch latch) {
        this(latch, null);
    }

    public MockRPMService(CountDownLatch latch, MockHarvestListener mockHarvestListener) {
        this.latch = latch;
        this.mockHarvestListener = mockHarvestListener;
    }

    public ConnectionConfigListener getConnectionConfigListener() {
        return connectionConfigListener;
    }

    public void setConnectionConfigListener(ConnectionConfigListener connectionConfigListener) {
        this.connectionConfigListener = connectionConfigListener;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void setErrorService(ErrorService errorService) {
        this.errorService = errorService;
    }

    public int getRestartCount() {
        return restartCount;
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        for (ProfileData p : profiles) {
            if (p instanceof IProfile) this.profiles.add((IProfile) p);
            else if (p instanceof com.newrelic.agent.profile.v2.IProfile) this.profilesV2.add((com.newrelic.agent.profile.v2.IProfile)p);
        }
        return Collections.emptyList();
    }

    public List<IProfile> getProfiles() {
        return profiles;
    }

    public List<com.newrelic.agent.profile.v2.IProfile> getProfilesV2() {
        return profilesV2;
    }

    @Override
    public String getHostString() {
        return "newrelic.com";
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void launch() throws Exception {
        if (latch != null) {
            latch.countDown();
        }
    }

    public void setIsConnected(boolean value) {
        isConnected = value;
    }

    @Override
    public void harvest(StatsEngine statsEngine) throws Exception {
        if (latch != null) {
            latch.countDown();
        }
        if (mockHarvestListener != null) {
            mockHarvestListener.mockHarvest(statsEngine);
        }
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        this.sqlTraces = sqlTraces;
    }

    public List<SqlTrace> getSqlTraces() {
        return sqlTraces;
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        this.traces = traces;
        this.transactionTracesSeen.addAndGet(traces.size());
    }

    public List<TransactionTrace> getTraces() {
        return traces;
    }

    @Override
    public String getApplicationName() {
        if (appNames == null) {
            return appName.get();
        }
        return appNames.get(0);
    }

    public void setApplicationName(String appName) {
        this.appName.set(appName);
    }

    public List<String> getApplicationNames() {
        return appNames;
    }

    public void setApplicationNames(List<String> appNames) {
        this.appNames = appNames;
    }

    @Override
    public void reconnect() {
        restartCount++;
    }

    @Override
    public ErrorService getErrorService() {
        return errorService;
    }

    @Override
    public boolean isMainApp() {
        return true;
    }

    @Override
    public boolean hasEverConnected() {
        return everConnected;
    }

    public void setEverConnected(boolean everConnected) {
        this.everConnected = everConnected;
    }

    private Exception ex;

    @Override
    public void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<TransactionEvent> events) throws Exception {
        if (ex != null) {
            throw ex;
        }
        this.events.addAll(events);
        this.transactionEventsSeen.addAndGet(eventsSeen);
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception {
        this.events.addAll(events);
        this.spanEventsSeen.addAndGet(eventsSeen);
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
        this.events.addAll(events);
        this.customEventsSeen.addAndGet(eventsSeen);
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        this.events.addAll(events);
        this.logSenderEventsSeen.addAndGet(events.size());
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> events) throws Exception {
        this.events.addAll(events);
        this.errorEventsSeen.addAndGet(eventsSeen);
    }

    @Override
    public void sendErrorData(List<TracedError> tracedErrors) throws Exception {
        this.errorTraces.addAll(tracedErrors);
        this.errorTracesSeen.addAndGet(tracedErrors.size());
    }

    @Override
    public HealthDataProducer getHttpDataSenderAsHealthDataProducer() {
        return null;
    }

    // Mock only
    public void setSendAnalyticsEventsException(Exception e) {
        this.ex = e;
    }

    // Mock only
    public void clearSendAnalyticsEventsException() {
        this.ex = null;
    }

    // Mock only
    public Collection<AnalyticsEvent> getEvents() {
        return events;
    }

    public int getTransactionEventsSeen() {
        return transactionEventsSeen.get();
    }

    public int getSpanEventsSeen() {
        return spanEventsSeen.get();
    }

    public int getCustomEventsSeen() {
        return customEventsSeen.get();
    }

    public int getLogSenderEventsSeen() {
        return logSenderEventsSeen.get();
    }

    public int getErrorEventsSeen() {
        return errorEventsSeen.get();
    }

    public int getErrorTracesSeen() {
        return errorTracesSeen.get();
    }

    public int getTransactionTracesSeen() {
        return transactionTracesSeen.get();
    }

    // Mock only
    public void clearEvents() {
        events.clear();
        errorTraces.clear();
        spanEventsSeen.set(0);
        transactionEventsSeen.set(0);
        customEventsSeen.set(0);
        logSenderEventsSeen.set(0);
        errorTracesSeen.set(0);
        transactionTracesSeen.set(0);
    }

}
