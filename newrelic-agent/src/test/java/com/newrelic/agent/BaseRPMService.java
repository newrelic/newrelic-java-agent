/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableList;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.JarData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transaction.TransactionNamingScheme;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class BaseRPMService implements IRPMService {

    @Override
    public List<List<?>> getAgentCommands() throws Exception {
        return ImmutableList.of();
    }

    @Override
    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public String getEntityGuid() {
        return "";
    }

    @Override
    public String getApplicationName() {
        return "test";
    }

    @Override
    public String getApplicationLink() {
        return null;
    }

    @Override
    public String getHostString() {
        return "hostname";
    }

    @Override
    public void reconnect() {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isMainApp() {
        return true;
    }

    @Override
    public IAgentLogger getLogger() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void launch() throws Exception {
    }

    @Override
    public void harvest(StatsEngine statsEngine) throws Exception {
    }

    @Override
    public void harvestNow() {
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isStartedOrStarting() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isStoppedOrStopping() {
        return false;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public TransactionNamingScheme getTransactionNamingScheme() {
        return TransactionNamingScheme.LEGACY;
    }

    @Override
    public long getConnectionTimestamp() {
        return 0;
    }

    @Override
    public void sendModules(List<JarData> jarDataList) throws Exception {
    }

    @Override
    public void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<TransactionEvent> events) throws Exception {
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> events) throws Exception {
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, final Collection<SpanEvent> events) throws Exception {
    }

    @Override
    public void sendErrorData(List<TracedError> tracedErrors) throws Exception {
    }

    @Override
    public ErrorService getErrorService() {
        return null;
    }

    @Override
    public boolean hasEverConnected() {
        return false;
    }

    @Override
    public void addAgentConnectionEstablishedListener(AgentConnectionEstablishedListener listener) {

    }
}
