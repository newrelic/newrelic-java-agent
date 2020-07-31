/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class InfiniteTracingSpanEventsConfig extends BaseConfig {

    public static final String ROOT = "span_events";
    public static final String QUEUE_SIZE = "queue_size";

    public static final int DEFAULT_SPAN_EVENTS_QUEUE_SIZE = 100000;

    private final int queue_size;

    public InfiniteTracingSpanEventsConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");
        queue_size = getIntProperty(QUEUE_SIZE, DEFAULT_SPAN_EVENTS_QUEUE_SIZE);
    }

    public int getQueueSize() {
        return queue_size;
    }
}
