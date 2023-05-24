/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;

import java.util.Map;
import java.util.logging.Level;

/**
 * Filters attributes. While this filter does provide boolean on whether request parameter and message parameters should
 * be filtered, there is no concept of a user vs agent attribute here. This means for high security, user attributes
 * need to be turned off elsewhere.
 */
public class AttributesFilter {

    // booleans to optimize the collection of request/message parameters
    private final boolean captureRequestParameters;
    private final boolean captureMessageParameters;

    private final DestinationFilter rootFilter;
    private final DestinationFilter errorEventFilter;
    private final DestinationFilter transactionEventFilter;
    private final DestinationFilter transactionTraceFilter;
    private final DestinationFilter spanEventFilter;
    private final DestinationFilter transactionSegmentFilter;
    private final DestinationFilter browserFilter;
    private final Map<String, DestinationFilter> destinationFilterMap;

    private static final String[] DEFAULT_ERROR_EVENTS_EXCLUDES = new String[] {};
    private static final String[] DEFAULT_TRANSACTION_TRACES_EXCLUDES = new String[] {};
    private static final String[] DEFAULT_TRANSACTION_SEGMENTS_EXCLUDES = new String[] {};
    private static final String[] DEFAULT_SPAN_EVENTS_EXCLUDES = new String[] {};

    private static final String[] DEFAULT_BROWSER_EXCLUDES = new String[] {
            AttributeNames.DISPLAY_HOST,
            AttributeNames.HTTP_REQUEST_STAR,
            AttributeNames.INSTANCE_NAME,
            AttributeNames.JVM_STAR,
            AttributeNames.MESSAGE_REQUEST_STAR,
            AttributeNames.REQUEST_REFERER_PARAMETER_NAME,
            AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME,
            AttributeNames.REQUEST_HOST_PARAMETER_NAME,
            AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME,
            AttributeNames.REQUEST_METHOD_PARAMETER_NAME,
            AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME,
            AttributeNames.RESPONSE_CONTENT_TYPE_PARAMETER_NAME,
            AttributeNames.SOLR_STAR,
    };

    private static final String[] DEFAULT_TRANSACTION_EVENTS_EXCLUDES = new String[] {
            AttributeNames.HTTP_REQUEST_STAR,
            AttributeNames.JVM_STAR,
            AttributeNames.MESSAGE_REQUEST_STAR,
            AttributeNames.SOLR_STAR
    };

    public AttributesFilter(AgentConfig config) {
        this(config, DEFAULT_BROWSER_EXCLUDES, DEFAULT_ERROR_EVENTS_EXCLUDES,
                DEFAULT_TRANSACTION_EVENTS_EXCLUDES, DEFAULT_TRANSACTION_TRACES_EXCLUDES,
                DEFAULT_SPAN_EVENTS_EXCLUDES, DEFAULT_TRANSACTION_SEGMENTS_EXCLUDES);
    }

    // have this constructor for testing
    public AttributesFilter(AgentConfig config, String[] defaultExcludeBrowser, String[] defaultExcludeErrorEvents, String[] defaultExcludeTransactionEvents,
            String[] defaultExcludeTransactionTraces, String[] defaultExcludeSpanEvents, String[] defaultExcludeTransactionSegments) {

        rootFilter = new DestinationFilter(AgentConfigImpl.ATTRIBUTES, true, config, defaultExcludeErrorEvents,
                AgentConfigImpl.ATTRIBUTES);

        errorEventFilter = new DestinationFilter(AgentConfigImpl.ERROR_COLLECTOR, true, config, defaultExcludeErrorEvents,
                AgentConfigImpl.ERROR_COLLECTOR);

        transactionEventFilter = new DestinationFilter(AgentConfigImpl.TRANSACTION_EVENTS, true, config, defaultExcludeTransactionEvents,
                AgentConfigImpl.TRANSACTION_EVENTS);

        transactionTraceFilter = new DestinationFilter(AgentConfigImpl.TRANSACTION_TRACER, true, config, defaultExcludeTransactionTraces,
                AgentConfigImpl.TRANSACTION_TRACER);

        // browser - default is false
        browserFilter = new DestinationFilter(AgentConfigImpl.BROWSER_MONITORING, false, config, defaultExcludeBrowser,
                AgentConfigImpl.BROWSER_MONITORING);

        spanEventFilter = new DestinationFilter(AgentConfigImpl.SPAN_EVENTS, true, config, defaultExcludeSpanEvents,
                AgentConfigImpl.SPAN_EVENTS);

        transactionSegmentFilter = new DestinationFilter(AgentConfigImpl.TRANSACTION_SEGMENTS, true, config, defaultExcludeTransactionSegments,
                AgentConfigImpl.TRANSACTION_SEGMENTS);

        destinationFilterMap = ImmutableMap.<String, DestinationFilter>builder()
                .put(AgentConfigImpl.ATTRIBUTES, rootFilter)
                .put(AgentConfigImpl.ERROR_COLLECTOR, errorEventFilter)
                .put(AgentConfigImpl.TRANSACTION_EVENTS, transactionEventFilter)
                .put(AgentConfigImpl.TRANSACTION_TRACER, transactionTraceFilter)
                .put(AgentConfigImpl.BROWSER_MONITORING, browserFilter)
                .put(AgentConfigImpl.SPAN_EVENTS, spanEventFilter)
                .put(AgentConfigImpl.TRANSACTION_SEGMENTS, transactionSegmentFilter)
                .build();

        // browser is not included in this list because we will never send request params to browser monitoring
        // since the request parameters are pulled in the do finish
        boolean enabled = errorEventFilter.isEnabled() || transactionEventFilter.isEnabled() || transactionTraceFilter.isEnabled() ||
                spanEventFilter.isEnabled() || transactionSegmentFilter.isEnabled();
        captureRequestParameters = captureAllParams(enabled, config.isHighSecurity(), "request.parameters.");
        captureMessageParameters = captureAllParams(enabled, config.isHighSecurity(), "message.parameters.");
    }

    /*
     * Returns true if we definitely need to capture parameters (request or message depending on the paramStart). In
     * order to not make the logic too complex, there are cases when this will return true but not actually capture any
     * parameters.
     */
    private boolean captureAllParams(boolean enabled, boolean highSecurity, String paramStart) {
        if (!enabled || highSecurity) {
            return false;
        } else {
            return errorEventFilter.isPotentialConfigMatch(paramStart)
                    || transactionEventFilter.isPotentialConfigMatch(paramStart)
                    || transactionTraceFilter.isPotentialConfigMatch(paramStart)
                    || browserFilter.isPotentialConfigMatch(paramStart)
                    || spanEventFilter.isPotentialConfigMatch(paramStart)
                    || transactionSegmentFilter.isPotentialConfigMatch(paramStart);
        }
    }

    public boolean captureRequestParams() {
        return captureRequestParameters;
    }

    public boolean captureMessageParams() {
        return captureMessageParameters;
    }

    public boolean isAttributesEnabledForErrorEvents() {
        return errorEventFilter.isEnabled();
    }

    public boolean isAttributesEnabledForTransactionEvents() {
        return transactionEventFilter.isEnabled();
    }

    public boolean isAttributesEnabledForTransactionTraces() {
        return transactionTraceFilter.isEnabled();
    }

    public boolean isAttributesEnabledForBrowser() {
        return browserFilter.isEnabled();
    }

    public boolean isAttributesEnabledForSpanEvents() {
        return spanEventFilter.isEnabled();
    }

    public boolean isAttributesEnabledForTransactionSegments() {
        return transactionSegmentFilter.isEnabled();
    }

    public Map<String, ?> filterErrorEventAttributes(Map<String, ?> values) {
        return errorEventFilter.filterAttributes(values);
    }

    public Map<String, ?> filterTransactionEventAttributes(Map<String, ?> values) {
        return transactionEventFilter.filterAttributes(values);
    }

    public Map<String, ?> filterTransactionTraceAttributes(Map<String, Object> values) {
        return transactionTraceFilter.filterAttributes(values);
    }

    public Map<String, ?> filterBrowserAttributes(Map<String, Object> values) {
        return browserFilter.filterAttributes(values);
    }

    public Map<String, ?> filterSpanEventAttributes(Map<String, ?> values) {
        return spanEventFilter.filterAttributes(values);
    }

    public Map<String, ?> filterTransactionSegmentAttributes(Map<String, Object> values) {
        return transactionSegmentFilter.filterAttributes(values);
    }

    public Map<String, ?> filterAttributes(Map<String, Object> values) {
        return rootFilter.filterAttributes(values);
    }

    public Map<String, ?> filterAttributesForDestination(Map<String, Object> values, String destination) {
        DestinationFilter destinationFilter = destinationFilterMap.get(destination);
        if (destinationFilter == null) {
            AgentBridge.getAgent().getLogger().log(Level.SEVERE, "Invalid destination for attribute filter {0}. Attributes are not filtered.", destination);
            return values;
        }
        return destinationFilter.filterAttributes(values);
    }

    public boolean shouldIncludeSpanAttribute(String attributeName) {
        return spanEventFilter.shouldIncludeAttribute(attributeName);
    }

    public boolean shouldIncludeErrorAttribute(String attributeName) {
        return errorEventFilter.shouldIncludeAttribute(attributeName);
    }
}
