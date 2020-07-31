/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Determines which attributes can be sent to the various destinations. Current destinations are transaction traces,
 * error collector, transaction events, and browser monitoring.
 */
public class AttributesService extends AbstractService implements AgentConfigListener {
    private final boolean enabled;
    private final String defaultAppName;
    // The default filter to use. This is always used when auto app naming is not set
    private volatile AttributesFilter defaultFilter;
    private final Map<String, AttributesFilter> appNamesToFilters;
    private final Map<String, Object> requestUriDummyMap = ImmutableMap.<String, Object>of(AttributeNames.REQUEST_URI, "");

    public AttributesService() {
        super(AttributesService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        enabled = config.getAttributesConfig().isEnabledRoot();
        defaultAppName = config.getApplicationName();
        defaultFilter = new AttributesFilter(config);
        appNamesToFilters = new ConcurrentHashMap<>();
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
    }

    public boolean captureRequestParams(String appName) {
        return getFilter(appName).captureRequestParams();
    }

    public boolean captureMessageParams(String appName) {
        return getFilter(appName).captureMessageParams();
    }

    public boolean isAttributesEnabledForErrorEvents(String appName) {
        return getFilter(appName).isAttributesEnabledForErrorEvents();
    }

    public boolean isAttributesEnabledForTransactionEvents(String appName) {
        return getFilter(appName).isAttributesEnabledForTransactionEvents();
    }

    public boolean isAttributesEnabledForTransactionTraces(String appName) {
        return getFilter(appName).isAttributesEnabledForTransactionTraces();
    }

    public boolean isAttributesEnabledForBrowser(String appName) {
        return getFilter(appName).isAttributesEnabledForBrowser();
    }

    public boolean isAttributesEnabledForSpanEvents(String appName) {
        return getFilter(appName).isAttributesEnabledForSpanEvents();
    }

    public boolean isAttributesEnabledForTransactionSegments(String appName) {
        return getFilter(appName).isAttributesEnabledForTransactionSegments();
    }

    /**
     * Filter attributes based on top-level rules only.
     *
     * Please make sure you're using the correct API.
     *
     * There are more specific APIs:
     *
     * {@link #filterTransactionTraceAttributes(String, Map)},
     * {@link #filterErrorEventAttributes(String, Map)}
     * {@link #filterTransactionEventAttributes(String, Map)}
     * {@link #filterBrowserAttributes(String, Map)}
     * {@link #filterSpanEventAttributes(String, Map)}
     * {@link #filterTransactionSegmentAttributes(String, Map)}
     *
     * @param appName Application name.
     * @param values Values to filter.
     * @return Map of filtered values.
     */
    public Map<String, ?> filterAttributes(String appName, Map<String, Object> values) {
        return getFilter(appName).filterAttributes(values);
    }

    public Map<String, ?> filterErrorEventAttributes(String appName, Map<String, ?> values) {
        return getFilter(appName).filterErrorEventAttributes(values);
    }

    public Map<String, ?> filterTransactionEventAttributes(String appName, Map<String, ?> values) {
        return getFilter(appName).filterTransactionEventAttributes(values);
    }

    public Map<String, ?> filterTransactionTraceAttributes(String appName, Map<String, Object> values) {
        return getFilter(appName).filterTransactionTraceAttributes(values);
    }

    public Map<String, ?> filterBrowserAttributes(String appName, Map<String, Object> values) {
        return getFilter(appName).filterBrowserAttributes(values);
    }

    public Map<String, ?> filterSpanEventAttributes(String appName, Map<String, ?> values) {
        return getFilter(appName).filterSpanEventAttributes(values);
    }

    public Map<String, ?> filterTransactionSegmentAttributes(String appName, Map<String, Object> values) {
        return getFilter(appName).filterTransactionSegmentAttributes(values);
    }

    public boolean shouldIncludeSpanAttribute(String appName, String attributeName) {
        return getFilter(appName).shouldIncludeSpanAttribute(attributeName);
    }

    public boolean shouldIncludeErrorAttribute(String appName, String attributeName) {
        return getFilter(appName).shouldIncludeErrorAttribute(attributeName);
    }

    private AttributesFilter getFilter(String appName) {
        if (appName == null || appName.equals(defaultAppName)) {
            return defaultFilter;
        } else {
            AttributesFilter filter = appNamesToFilters.get(appName);
            return (filter == null) ? defaultFilter : filter;
        }
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        // create a new filter any time the configuration has changed for the application name
        if (appName != null) {
            if (appName.equals(defaultAppName)) {
                defaultFilter = new AttributesFilter(agentConfig);
            } else {
                appNamesToFilters.put(appName, new AttributesFilter(agentConfig));
            }
        }
    }

    public String filterRequestUri(String appName, String destination, String uri) {
        Map<String, ?> filteredUriMap = getFilter(appName).filterAttributesForDestination(requestUriDummyMap, destination);
        return filteredUriMap.containsKey(AttributeNames.REQUEST_URI) ? uri : null;
    }
}
