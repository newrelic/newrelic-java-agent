/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.EventService;

public interface SpanEventsService extends EventService {

    void storeEvent(SpanEvent event);

    void addHarvestableToService(String appName);

    SamplingPriorityQueue<SpanEvent> getOrCreateDistributedSamplingReservoir(String appName);
}
