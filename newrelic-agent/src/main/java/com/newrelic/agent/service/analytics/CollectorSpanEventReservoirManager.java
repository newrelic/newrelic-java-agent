/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.ComparisonChain;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.api.agent.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CollectorSpanEventReservoirManager implements ReservoirManager<SpanEvent> {

    private final ConfigService configService;
    private ConcurrentHashMap<String, SamplingPriorityQueue<SpanEvent>> spanReservoirsForApp = new ConcurrentHashMap<>();
    private volatile int maxSamplesStored;

    public CollectorSpanEventReservoirManager(ConfigService configService) {
        this.configService = configService;
        maxSamplesStored = configService.getDefaultAgentConfig().getSpanEventsConfig().getMaxSamplesStored();
    }

    @Override
    public SamplingPriorityQueue<SpanEvent> getOrCreateReservoir(String appName) {
       SamplingPriorityQueue<SpanEvent> reservoir = spanReservoirsForApp.get(appName);
        if (reservoir == null) {
            reservoir = spanReservoirsForApp.putIfAbsent(appName, createDistributedSamplingReservoir(appName));
            if (reservoir == null) {
                reservoir = spanReservoirsForApp.get(appName);
            }
        }
        return reservoir;
    }

    private SamplingPriorityQueue<SpanEvent> createDistributedSamplingReservoir(String appName) {
        return new DistributedSamplingPriorityQueue<>(appName, "Span Event Service", maxSamplesStored, SPAN_EVENT_COMPARATOR);
    }

    @Override
    public void clearReservoir() {
        spanReservoirsForApp.clear();
    }

    @Override
    public HarvestResult attemptToSendReservoir(final String appName, EventSender<SpanEvent> eventSender, Logger logger) {
        if (getMaxSamplesStored() <= 0) {
            clearReservoir();
            return null;
        }

        SpanEventsConfig config = configService.getAgentConfig(appName).getSpanEventsConfig();

        // save a reference to the old reservoir to finish harvesting, and create a new one
        final SamplingPriorityQueue<SpanEvent> toSend = spanReservoirsForApp.get(appName);

        if (toSend != null){
            toSend.logReservoirStats();
        }

        spanReservoirsForApp.put(appName, createDistributedSamplingReservoir(appName));

        if (toSend == null || toSend.size() <= 0) {
            return null;
        }

        try {
            eventSender.sendEvents(appName, config.getMaxSamplesStored(), toSend.getNumberOfTries(), Collections.unmodifiableList(toSend.asList()));
            if (toSend.size() < toSend.getNumberOfTries()) {
                int dropped = toSend.getNumberOfTries() - toSend.size();
                logger.log(Level.FINE, "Dropped {0} span events out of {1}.", dropped, toSend.getNumberOfTries());
            }
            return new HarvestResult(toSend.getNumberOfTries(), toSend.size());
        } catch (HttpError e) {
            if (!e.discardHarvestData()) {
                logger.log(Level.FINE, "Unable to send span events. Unsent events will be included in the next harvest.", e);
                // Save unsent data by merging it with toSend data using reservoir algorithm
                spanReservoirsForApp.get(appName).retryAll(toSend);
            } else {
                // discard harvest data
                toSend.clear();
                logger.log(Level.FINE, "Unable to send span events. Unsent events will be dropped.", e);
            }
        } catch (Exception e) {
            // discard harvest data
            toSend.clear();
            logger.log(Level.FINE, "Unable to send span events. Unsent events will be dropped.", e);
        }
        return null;
    }

    @Override
    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    @Override
    public void setMaxSamplesStored(int newMax) {
        maxSamplesStored = newMax;
        ConcurrentHashMap<String, SamplingPriorityQueue<SpanEvent>> newMaxSpanReservoirs = new ConcurrentHashMap<>();
        spanReservoirsForApp.forEach((appName,reservoir ) -> newMaxSpanReservoirs.putIfAbsent(appName, createDistributedSamplingReservoir(appName)));
        spanReservoirsForApp = newMaxSpanReservoirs;
    }

    // This is where you can add secondary sorting for Span Events
    private static final Comparator<SpanEvent> SPAN_EVENT_COMPARATOR = (left, right) -> ComparisonChain.start()
            .compare(right.getPriority(), left.getPriority()) // Take highest priority first
            .result();

}
