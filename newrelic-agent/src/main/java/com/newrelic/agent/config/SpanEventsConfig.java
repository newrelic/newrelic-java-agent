/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class SpanEventsConfig extends BaseConfig {

    public static final int DEFAULT_MAX_SPAN_EVENTS_PER_HARVEST = 2000;
    public static final int DEFAULT_TARGET_SAMPLES_STORED = 10;

    public static final String COLLECT_SPAN_EVENTS = "collect_span_events";
    public static final String ENABLED = "enabled";
    public static final String MAX_SPAN_EVENTS_PER_HARVEST = "max_samples_stored";
    public static final String SERVER_SPAN_HARVEST_CONFIG = "span_event_harvest_config";
    public static final String SERVER_SPAN_HARVEST_LIMIT = "harvest_limit";
    private static final String ROOT = "newrelic.config.";
    private static final String SPAN_EVENTS = "span_events.";
    public static final String SYSTEM_PROPERTY_ROOT = ROOT + SPAN_EVENTS;
    private static final String TARGET_SAMPLES_STORED = "target_samples_stored";
    private static final boolean DEFAULT_COLLECT_SPANS = false;

    private final boolean dtEnabled;
    private int maxSamplesStored;
    private final boolean enabled;
    private final int targetSamplesStored;

    public SpanEventsConfig(Map<String, Object> props, boolean dtEnabled) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.dtEnabled = dtEnabled;
        this.maxSamplesStored = getProperty(MAX_SPAN_EVENTS_PER_HARVEST, DEFAULT_MAX_SPAN_EVENTS_PER_HARVEST);
        this.enabled = initEnabled(maxSamplesStored);
        this.targetSamplesStored = getProperty(TARGET_SAMPLES_STORED, DEFAULT_TARGET_SAMPLES_STORED);
    }

    private boolean initEnabled(int maxSamplesStored) {
        boolean configEnabled = getProperty(ENABLED, dtEnabled) && dtEnabled;
        boolean collectSpanEventsFromCollector = getProperty(COLLECT_SPAN_EVENTS, DEFAULT_COLLECT_SPANS);
        return maxSamplesStored > 0 && configEnabled && collectSpanEventsFromCollector;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public void setMaxSamplesStoredByServerProp(int harvestLimit) {
        this.maxSamplesStored = harvestLimit;
    }

    public int getTargetSamplesStored() {
        return targetSamplesStored;
    }

}
