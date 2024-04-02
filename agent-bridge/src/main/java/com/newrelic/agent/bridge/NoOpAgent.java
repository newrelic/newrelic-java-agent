/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.AiMonitoring;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.ErrorApi;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.TraceMetadata;

import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.bridge.NoOpAiMonitoring.INSTANCE;

class NoOpAgent implements Agent {

    static final Agent INSTANCE = new NoOpAgent();

    private NoOpAgent() {
    }

    @Override
    public Logger getLogger() {
        return NoOpLogger.INSTANCE;
    }

    @Override
    public Config getConfig() {
        return NoOpConfig.Instance;
    }

    @Override
    public TracedMethod getTracedMethod() {
        return NoOpTracedMethod.INSTANCE;
    }

    @Override
    public Transaction getTransaction() {
        return NoOpTransaction.INSTANCE;
    }

    @Override
    public Transaction getTransaction(boolean createIfNotExists) {
        return NoOpTransaction.INSTANCE;
    }

    @Override
    public Transaction getWeakRefTransaction(boolean createIfNotExists) {
        return NoOpTransaction.INSTANCE;
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return NoOpMetricAggregator.INSTANCE;
    }

    @Override
    public Insights getInsights() {
        return NoOpInsights.INSTANCE;
    }

    @Override
    public AiMonitoring getAiMonitoring() {
        return NoOpAiMonitoring.INSTANCE;
    }

    @Override
    public ErrorApi getErrorApi() {
        return NoOpErrorApi.INSTANCE;
    }

    @Override
    public Logs getLogSender() {
        return NoOpLogs.INSTANCE;
    }

    @Override
    public String getEntityGuid(boolean wait) {
        return null;
    }

    @Override
    public boolean startAsyncActivity(Object activityContext) {
        return false;
    }

    @Override
    public boolean ignoreIfUnstartedAsyncContext(Object activityContext) {
        return false;
    }

    @Override
    public TraceMetadata getTraceMetadata() {
        return NoOpTraceMetadata.INSTANCE;
    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        return Collections.emptyMap();
    }
}
