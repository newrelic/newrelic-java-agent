/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.interfaces;

import com.newrelic.agent.model.PriorityAware;
import com.newrelic.api.agent.Logger;

import java.util.List;

/**
 * Manages a pool of events
 */
public interface ReservoirManager<T extends PriorityAware> {

    SamplingPriorityQueue<T> getOrCreateReservoir(String appName);

    void clearReservoir();

    HarvestResult attemptToSendReservoir(String appName, EventSender<T> eventSender, Logger logger);

    int getMaxSamplesStored();

    void setMaxSamplesStored(int newMax);

    interface EventSender<T> {
        void sendEvents(String appName, int reservoirSize, int eventsSeen, List<T> events) throws Exception;
    }

    class HarvestResult {
        public final int seen;
        public final int sent;

        public HarvestResult(int seen, int sent) {
            this.seen = seen;
            this.sent = sent;
        }
    }
}
