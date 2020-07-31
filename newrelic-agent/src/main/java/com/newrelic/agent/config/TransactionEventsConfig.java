/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class TransactionEventsConfig extends BaseConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.transaction_events.";
    public static final String MAX_SAMPLES_STORED = "max_samples_stored";
    public static final String CUSTOM_REQUEST_HEADERS = "custom_request_headers";
    private static final String COLLECT_ANALYTICS_EVENTS = "collect_analytics_events";
    private static final String ENABLED = "enabled";
    private static final String TARGET_SAMPLES_STORED = "target_samples_stored";

    public static final String REQUEST_HEADER_NAME = "header_name";
    public static final String HEADER_ALIAS = "header_alias";

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MAX_SAMPLES_STORED = 2000;
    public static final int DEFAULT_TARGET_SAMPLES_STORED = 10;

    private final boolean enabled;
    private final int maxSamplesStored;
    private final int targetSamplesStored;
    private final Set<CustomRequestHeaderConfig> requestHeaderConfigs;

    public TransactionEventsConfig(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        maxSamplesStored = getProperty(MAX_SAMPLES_STORED, DEFAULT_MAX_SAMPLES_STORED);
        enabled = maxSamplesStored > 0 & initEnabled();
        targetSamplesStored = getProperty(TARGET_SAMPLES_STORED, DEFAULT_TARGET_SAMPLES_STORED);
        requestHeaderConfigs = initCustomRequestHeaders();
    }

    private boolean initEnabled() {
        // "collect_analytics_events" is the property which comes down from the server.
        // This gets mapped to transaction_events.collect_analytics_events in AgentConfigFactory.mergeServerData()
        return getProperty(ENABLED, DEFAULT_ENABLED) && getProperty(COLLECT_ANALYTICS_EVENTS, DEFAULT_ENABLED);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public Set<CustomRequestHeaderConfig> getRequestHeaderConfigs() {
        return requestHeaderConfigs;
    }

    public int getTargetSamplesStored() {
        return targetSamplesStored;
    }

    public Set<CustomRequestHeaderConfig> initCustomRequestHeaders() {
        Set<CustomRequestHeaderConfig> headerConfigs = new HashSet<>();

        Object customRequestHeader = getProperty(CUSTOM_REQUEST_HEADERS);
        if (customRequestHeader instanceof Collection) {
            for (Object header : (Collection) customRequestHeader) {
                if (header instanceof Map) {
                    Map<String, String> customRequestHeaderMap = (Map) header;
                    String name = customRequestHeaderMap.get(REQUEST_HEADER_NAME);
                    String alias = customRequestHeaderMap.get(HEADER_ALIAS);
                    // header_name cannot be null, but the alias is optional and thus can be null
                    if (name != null) {
                        if (alias != null && !alias.isEmpty()) {
                            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_CUSTOM_REQUEST_HEADER_ALIAS);
                        } else {
                            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_CUSTOM_REQUEST_HEADER);
                        }
                        headerConfigs.add(new CustomRequestHeaderConfigImpl(name, alias));
                    } else {
                        AgentBridge.getAgent().getLogger().log(Level.WARNING, "Invalid customer_request_header config" +
                                " encountered. header_name must not be null. This configuration will be ignored");
                    }
                } else if (header instanceof String) {
                    String name = (String) header;
                    headerConfigs.add(new CustomRequestHeaderConfigImpl(name, null));
                    MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_CUSTOM_REQUEST_HEADER);
                }
            }

        }
        return headerConfigs;
    }

    public static TransactionEventsConfig createTransactionEventConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new TransactionEventsConfig(settings);
    }
}
