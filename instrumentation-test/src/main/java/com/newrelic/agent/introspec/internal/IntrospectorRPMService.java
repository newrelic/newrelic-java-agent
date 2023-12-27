/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.JarData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transaction.TransactionNamingScheme;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class IntrospectorRPMService extends AbstractService implements IRPMService {

    private ErrorService errorService;

    protected IntrospectorRPMService() {
        super(IntrospectorRPMService.class.getName());
        errorService = new IntrospectorErrorService();
    }

    @Override
    public boolean isEnabled() {
        return true;
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
    public List<Long> sendProfileData(List<ProfileData> profiles) {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void launch() {
    }

    @Override
    public String getHostString() {
        return null;
    }

    @Override
    public void harvest(StatsEngine statsEngine) {
    }

    @Override
    public void harvestNow() {
    }

    @Override
    public List<List<?>> getAgentCommands() {
        return null;
    }

    @Override
    public void sendCommandResults(Map<Long, Object> commandResults) {
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) {
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) {
    }

    @Override
    public String getApplicationLink() {
        return null;
    }

    @Override
    public String getApplicationName() {
        return "TestApp";
    }

    @Override
    public void reconnect() {
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
        return false;
    }

    @Override
    public TransactionNamingScheme getTransactionNamingScheme() {
        return null;
    }

    @Override
    public long getConnectionTimestamp() {
        return 0;
    }

    @Override
    public void sendModules(List<JarData> jarDataList) {
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, final Collection<SpanEvent> events) {
    }

    @Override
    public void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<TransactionEvent> events) {
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) {
    }

    @Override
    public void sendDimensionalMetricData(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> metricData) {
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) {
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> events) {
    }

    @Override
    public void sendErrorData(List<TracedError> tracedErrors) {
    }

    @Override
    public String getEntityGuid() {
        return "";
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

}
