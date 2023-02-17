/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class InfiniteTracingConfigImpl extends BaseConfig implements InfiniteTracingConfig {

    public static final String ROOT = "infinite_tracing";
    public static final String TRACE_OBSERVER = "trace_observer";
    public static final String SPAN_EVENTS = "span_events";
    public static final String FLAKY_PERCENTAGE = "_flakyPercentage";
    public static final String FLAKY_CODE = "_flakyCode";
    public static final String USE_PLAINTEXT = "plaintext";
    public static final boolean DEFAULT_USE_PLAINTEXT = false;
    public static final String USE_COMPRESSION = "compression";
    public static final boolean DEFAULT_USE_COMPRESSION = true;
    public static final String USE_BATCHING = "batching";
    public static final boolean DEFAULT_USE_BATCHING = true;

    static final String SYSTEM_PROPERTY_ROOT = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + ROOT + ".";

    private final InfiniteTracingTraceObserverConfig traceObserverConfig;
    private final InfiniteTracingSpanEventsConfig spanEventsConfig;
    private final boolean autoAppNamingEnabled;

    public InfiniteTracingConfigImpl(Map<String, Object> props) {
        this(props, false);
    }

    public InfiniteTracingConfigImpl(Map<String, Object> props, boolean autoAppNamingEnabled) {
        super(props, SYSTEM_PROPERTY_ROOT);
        if ((props != null) && props.containsKey(FLAKY_PERCENTAGE)) {
            Agent.LOG.log(Level.WARNING, "Infinite Tracing config contains a flaky percentage value");
        }
        this.autoAppNamingEnabled = autoAppNamingEnabled;
        traceObserverConfig = createTracerObserverConfig();
        spanEventsConfig = createSpanEventsConfig();
    }

    private InfiniteTracingSpanEventsConfig createSpanEventsConfig() {
        Map<String, Object> spanEventProps = getProperty(SPAN_EVENTS, Collections.<String, Object>emptyMap());
        return new InfiniteTracingSpanEventsConfig(spanEventProps, SYSTEM_PROPERTY_ROOT);
    }

    private InfiniteTracingTraceObserverConfig createTracerObserverConfig() {
        Map<String, Object> traceObserverProps = getProperty(TRACE_OBSERVER, Collections.<String, Object>emptyMap());
        return new InfiniteTracingTraceObserverConfig(traceObserverProps, SYSTEM_PROPERTY_ROOT);
    }

    @Override
    public String getTraceObserverHost() {
        return traceObserverConfig.getHost();
    }

    @Override
    public int getTraceObserverPort() {
        return traceObserverConfig.getPort();
    }

    @Override
    public int getSpanEventsQueueSize() {
        return spanEventsConfig.getQueueSize();
    }

    @Override
    public Double getFlakyPercentage() {
        return getProperty(FLAKY_PERCENTAGE);
    }

    @Override
    public Long getFlakyCode() {
        return getProperty(FLAKY_CODE);
    }

    @Override
    public boolean getUsePlaintext() {
        return getProperty(USE_PLAINTEXT, DEFAULT_USE_PLAINTEXT);
    }

    @Override
    public boolean getUseCompression() {
        return getProperty(USE_COMPRESSION, DEFAULT_USE_COMPRESSION);
    }

    @Override
    public boolean getUseBatching() {
        return getProperty(USE_BATCHING, DEFAULT_USE_BATCHING);
    }

    @Override
    public boolean isEnabled() {
        if (!getTraceObserverHost().isEmpty() && autoAppNamingEnabled) {
            Agent.LOG.log(Level.WARNING, "Infinite Tracing is disabled because enable_auto_app_naming is set to true.");
            return false;
        }
        return !getTraceObserverHost().isEmpty();
    }
}
