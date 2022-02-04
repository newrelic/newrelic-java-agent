/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.interfaces;

import com.newrelic.agent.model.PriorityAware;
import com.newrelic.api.agent.Logger;

public class NoOpReservoirManager<T extends PriorityAware> implements ReservoirManager<T> {
    @Override
    public HarvestResult attemptToSendReservoir(String appName, EventSender<T> eventSender, Logger logger) {
        return null;
    }

    @Override
    public SamplingPriorityQueue<T> getOrCreateReservoir(String appName) {
        return null;
    }

    @Override
    public int getMaxSamplesStored() {
        return 0;
    }

    @Override
    public void setMaxSamplesStored(int newMax) {

    }

    @Override
    public void clearReservoir() {

    }
}
