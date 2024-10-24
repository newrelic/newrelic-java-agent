/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.JarData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.superagent.HealthDataProducer;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transaction.TransactionNamingScheme;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IRPMService extends Service {

    List<Long> sendProfileData(List<ProfileData> profiles) throws Exception;

    boolean isConnected();

    void launch() throws Exception;

    String getHostString();

    void harvest(StatsEngine statsEngine) throws Exception;

    void harvestNow();

    List<List<?>> getAgentCommands() throws Exception;

    void sendCommandResults(Map<Long, Object> commandResults) throws Exception;

    void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception;

    void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception;

    String getEntityGuid();

    String getApplicationName();

    String getApplicationLink();

    void reconnect();

    ErrorService getErrorService();

    boolean isMainApp();

    boolean hasEverConnected();

    TransactionNamingScheme getTransactionNamingScheme();

    /**
     * The timestamp of when the agent connected to the New Relic service.
     *
     */
    long getConnectionTimestamp();

    /**
     * Sends the meta information about the jars used by the application to the New Relic service.
     * Note that only the jars which have not yet been sent be passed into this method.
     *
     * @param jarDataList The jars which have not yet been sent to the New Relic service.
     * @throws Exception Thrown if a problem sending the jars.
     */
    void sendModules(List<JarData> jarDataList) throws Exception;

    void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<TransactionEvent> events) throws Exception;

    void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception;

    void sendLogEvents(Collection<? extends LogEvent> events) throws Exception;

    void sendErrorEvents(int reservoirSize, int eventsSeen, final Collection<ErrorEvent> events) throws Exception;

    void sendSpanEvents(int reservoirSize, int eventsSeen, final Collection<SpanEvent> events) throws Exception;

    void sendErrorData(List<TracedError> tracedErrors) throws Exception;

    void addAgentConnectionEstablishedListener(AgentConnectionEstablishedListener listener);

    HealthDataProducer getHttpDataSenderAsHealthDataProducer();
}
