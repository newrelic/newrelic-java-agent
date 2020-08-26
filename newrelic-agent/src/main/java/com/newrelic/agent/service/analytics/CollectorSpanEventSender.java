/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;

import java.text.MessageFormat;
import java.util.List;

public class CollectorSpanEventSender implements ReservoirManager.EventSender<SpanEvent> {

    private final RPMServiceManager rpmServiceManager;

    public CollectorSpanEventSender(RPMServiceManager rpmServiceManager) {
        this.rpmServiceManager = rpmServiceManager;
    }

    @Override
    public void sendEvents(String appName, int reservoirSize, int eventsSeen, List<SpanEvent> events) throws Exception {
        rpmServiceManager
                .getOrCreateRPMService(appName)
                .sendSpanEvents(reservoirSize, eventsSeen, events);

        for(SpanEvent event: events) {
            ServiceFactory.getStatsService().doStatsWork(
                    StatsWorks.getIncrementCounterWork(MessageFormat.format(MetricNames.SUPPORTABILITY_FRAMEWORK_SPAN_USAGE, event.getInstrumentationModule()), 1));

            ServiceFactory.getStatsService().doStatsWork(
                    StatsWorks.getRecordMetricWork(MessageFormat.format(MetricNames.SUPPORTABILITY_FRAMEWORK_SPAN_BYTE_USAGE, event.getInstrumentationModule()), event.getSizeOfEventInBytes()));
        }
    }
}
