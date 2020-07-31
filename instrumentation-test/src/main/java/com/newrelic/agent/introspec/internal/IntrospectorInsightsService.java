/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.deps.com.google.common.collect.LinkedListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.ListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.agent.deps.com.google.common.collect.Multimaps;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.api.agent.Insights;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class IntrospectorInsightsService implements InsightsService, Insights {

    private static String SERVICE_NAME = "InsightsService";
    private ListMultimap<String, CustomInsightsEvent> events = Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, CustomInsightsEvent>create());

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
    public void recordCustomEvent(String eventType, Map<String, ?> attributes) {
        if (AnalyticsEvent.isValidType(eventType)) {
            Map<String, Object> atts = Maps.newHashMap(attributes);
            CustomInsightsEvent event = new CustomInsightsEvent(eventType, System.currentTimeMillis(), atts, DistributedTraceServiceImpl.nextTruncatedFloat());
            storeEvent("TestApp", event);
        }
    }

    @Override
    public Insights getTransactionInsights(AgentConfig config) {
        return this;
    }

    @Override
    public void storeEvent(String appName, CustomInsightsEvent event) {
        events.put(event.getType(), event);
    }

    @Override
    public void addHarvestableToService(String s) {
    }

    public Collection<String> getEventTypes() {
        return Collections.unmodifiableCollection(events.keys());
    }

    public Collection<Event> getEvents(String type) {
        List<CustomInsightsEvent> currentEvents = events.get(type);
        if (currentEvents == null) {
            return null;
        }
        List<Event> output = new ArrayList<>(currentEvents.size());
        for (CustomInsightsEvent current : currentEvents) {
            output.add(new EventImpl(current.getType(), current.getUserAttributesCopy()));
        }
        return output;
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
