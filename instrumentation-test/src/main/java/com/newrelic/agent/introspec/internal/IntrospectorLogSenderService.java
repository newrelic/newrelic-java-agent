/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.deps.com.google.common.collect.LinkedListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.ListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.agent.deps.com.google.common.collect.Multimaps;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.logging.LogSenderService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.api.agent.Logs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static com.newrelic.agent.model.LogEvent.LOG_EVENT_TYPE;

class IntrospectorLogSenderService implements LogSenderService {

    private static String SERVICE_NAME = "LogSenderService";
    private ListMultimap<String, LogEvent> events = Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, LogEvent>create());

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public IAgentLogger getLogger() {
        return Agent.LOG;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isStartedOrStarting() {
        return true;
    }

    @Override
    public boolean isStoppedOrStopping() {
        return false;
    }

    @Override
    public void recordLogEvent(Map<LogAttributeKey, ?> attributes) {
        if (AnalyticsEvent.isValidType(LOG_EVENT_TYPE)) {
            Map<String, Object> atts = attributes.entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().getPrefixedKey(), Map.Entry::getValue));
            LogEvent event = new LogEvent(atts, DistributedTraceServiceImpl.nextTruncatedFloat());
            storeEvent("TestApp", event);
        }
    }

    @Override
    public Logs getTransactionLogs(AgentConfig config) {
        return this;
    }

    @Override
    public void storeEvent(String appName, LogEvent event) {
        events.put(event.getType(), event);
    }

    @Override
    public void addHarvestableToService(String s) {
    }

    public Collection<LogEvent> getLogEvents() {
        return Collections.unmodifiableCollection(events.values());
    }

    public void clear() {
        events.clear();
    }

    @Override
    public void harvestEvents(String appName) {
    }

    @Override
    public String getEventHarvestIntervalMetric() {
        return "";
    }

    @Override
    public String getReportPeriodInSecondsMetric() {
        return "";
    }

    @Override
    public String getEventHarvestLimitMetric() {
        return "";
    }

    @Override
    public int getMaxSamplesStored() {
        return 0;
    }

    @Override
    public void setMaxSamplesStored(int maxSamplesStored) {
    }

    @Override
    public void clearReservoir() {
        events.clear();
    }
}
