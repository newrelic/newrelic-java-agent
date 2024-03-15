/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.AiMonitoring;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.ErrorApi;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.TraceMetadata;

import java.util.Map;

public class FakeExtensionAgent implements Agent {
    Logger logger;

    @Override
    public Logger getLogger() {
        return logger;
    }

    //<editor-fold desc="throw RuntimeException">
    @Override
    public Config getConfig() { throw new RuntimeException(); }

    @Override
    public MetricAggregator getMetricAggregator() { throw new RuntimeException(); }

    @Override
    public Insights getInsights() { throw new RuntimeException(); }

    @Override
    public AiMonitoring getAiMonitoring() {
        return null;
    }

    @Override
    public ErrorApi getErrorApi() { throw new RuntimeException(); }

    @Override
    public TraceMetadata getTraceMetadata() { throw new RuntimeException(); }

    @Override
    public Map<String, String> getLinkingMetadata() { throw new RuntimeException(); }

    @Override
    public TracedMethod getTracedMethod() { throw new RuntimeException(); }

    @Override
    public Transaction getTransaction() { throw new RuntimeException(); }

    @Override
    public Transaction getTransaction(boolean createIfNotExists) { throw new RuntimeException(); }

    @Override
    public Transaction getWeakRefTransaction(boolean createIfNotExists) { throw new RuntimeException(); }

    @Override
    public boolean startAsyncActivity(Object activityContext) { throw new RuntimeException(); }

    @Override
    public boolean ignoreIfUnstartedAsyncContext(Object activityContext) { throw new RuntimeException(); }

    @Override
    public Logs getLogSender() {
        throw new RuntimeException();
    }
}
