/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TracedError implements Comparable<TracedError>, JSONStreamAware {
    private final String path;
    private final long timestampInMillis;
    private final String requestUri;
    private final String appName;
    private final Map<String, Map<String, String>> prefixAtts;
    private final Map<String, ?> userAtts;
    private final Map<String, ?> agentAtts;
    private final Map<String, ?> errorAtts;
    private final Map<String, ?> intrinsics;

    final ErrorCollectorConfig errorCollectorConfig;
    final TransactionData transactionData;
    final boolean expected; // reported as expected error by API call

    final String transactionGuid;

    public abstract static class Builder {
        protected final ErrorCollectorConfig errorCollectorConfig;
        protected final String appName;
        protected final String frontendMetricName;
        protected final long timestampInMillis;

        protected TransactionData transactionData = null;
        protected String requestUri = "";
        protected Map<String, Map<String, String>> prefixedAttributes = Collections.emptyMap();
        protected Map<String, Object> userAttributes = Collections.emptyMap();
        protected Map<String, Object> agentAttributes = Collections.emptyMap();
        protected Map<String, ?> errorAttributes = Collections.emptyMap();
        protected Map<String, Object> intrinsicAttributes = Collections.emptyMap();

        protected boolean expected;

        protected String transactionGuid;

        Builder(ErrorCollectorConfig errorCollectorConfig, String appName, String frontendMetricName, long timestampInMillis) {
            this.errorCollectorConfig = errorCollectorConfig;
            this.appName = appName;
            this.frontendMetricName = frontendMetricName;
            this.timestampInMillis = timestampInMillis;
        }

        public Builder transactionData(TransactionData transactionData) {
            this.transactionData = transactionData;
            return this;
        }

        public Builder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public Builder prefixedAttributes(Map<String, Map<String, String>> prefixedAttributes) {
            this.prefixedAttributes = prefixedAttributes;
            return this;
        }

        public Builder userAttributes(Map<String, Object> userAttributes) {
            this.userAttributes = userAttributes;
            return this;
        }

        public Builder agentAttributes(Map<String, Object> agentAttributes) {
            this.agentAttributes = agentAttributes;
            return this;
        }

        public Builder errorAttributes(Map<String, ?> errorAttributes) {
            this.errorAttributes = errorAttributes;
            return this;
        }

        public Builder intrinsicAttributes(Map<String, Object> intrinsicAttributes) {
            this.intrinsicAttributes = intrinsicAttributes;
            return this;
        }

        public Builder expected(boolean expected) {
            this.expected = expected;
            return this;
        }

        public Builder transactionGuid(String transactionGuid) {
            this.transactionGuid = transactionGuid;
            return this;
        }

        public abstract TracedError build();
    }

    protected TracedError(ErrorCollectorConfig errorCollectorConfig, String appName, String frontendMetricName,
            long timestampInMillis, String requestUri, Map<String, Map<String, String>> prefixedParams,
            Map<String, ?> userParams, Map<String, ?> agentParams, Map<String, ?> errorParams,
            Map<String, ?> intrinsics, TransactionData transactionData, boolean expected, String transactionGuid) {
        this.errorCollectorConfig = errorCollectorConfig;
        this.appName = appName;
        this.path = frontendMetricName == null ? "Unknown" : frontendMetricName;
        this.requestUri = requestUri == null ? "Unknown" : requestUri;
        this.timestampInMillis = timestampInMillis;
        this.prefixAtts = setAtts(prefixedParams);
        this.userAtts = setAtts(userParams);
        this.agentAtts = setAtts(agentParams);
        this.errorAtts = setAtts(errorParams);
        this.intrinsics = setAtts(intrinsics);
        this.transactionData = transactionData;
        this.expected = expected;
        this.transactionGuid = transactionGuid;
    }

    private <K, V> Map<K, V> setAtts(Map<K, V> inputAtts) {
        if (inputAtts == null) {
            return Collections.emptyMap();
        }
        return inputAtts;
    }

    public abstract String getMessage();

    public abstract String getExceptionClass();

    public long getTimestampInMillis() {
        return timestampInMillis;
    }

    public String getPath() {
        return path;
    }

    public Map<String, ?> getErrorAtts() {
        return errorAtts;
    }

    public Map<String, ?> getIntrinsicAtts() {
        return intrinsics;
    }

    /**
     * Returns the stack trace associated with this error, or null if the error has no stack trace.
     */
    public abstract Collection<String> stackTrace();

    /**
     * Returns a map of multiple stack traces for this error. The key is the title of the stack trace.
     */
    public Map<String, Collection<String>> stackTraces() {
        return Collections.emptyMap();
    }

    private Map<String, Object> getUserAtts() {
        Map<String, Object> atts = new HashMap<>();
        atts.putAll(errorAtts);
        atts.putAll(userAtts);
        return atts;
    }

    public Map<String, Object> getAgentAtts() {
        Map<String, Object> atts = new HashMap<>();
        atts.putAll(agentAtts);
        if (prefixAtts != null && !prefixAtts.isEmpty()) {
            atts.putAll(AttributesUtils.appendAttributePrefixes(prefixAtts));
        }

        atts.put(AttributeNames.REQUEST_URI, requestUri);
        return atts;
    }

    private void filterAndAddIfNotEmpty(String key, Map<String, Object> wheretoAdd, Map<String, Object> toAdd) {
        Map<String, ?> output = ServiceFactory.getAttributesService().filterErrorEventAttributes(appName, toAdd);
        if (output != null && !output.isEmpty()) {
            wheretoAdd.put(key, output);
        }
    }

    private Map<String, ?> getAttributes() {
        Map<String, Object> params = new HashMap<>();

        if (ServiceFactory.getAttributesService().isAttributesEnabledForErrorEvents(appName)) {
            filterAndAddIfNotEmpty("agentAttributes", params, getAgentAtts());
            // user atts should have already been filtered for out if in high security - this is extra protection
            // high security is per an account - meaning it can not be different for various application names within a
            // JVM - so we can just check the default agent config
            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                filterAndAddIfNotEmpty("userAttributes", params, getUserAtts());
            }
        }

        Map<String, Object> tracedErrorIntrinsics = new HashMap<>(intrinsics);
        if (!incrementsErrorMetric() && !(this instanceof DeadlockTraceError)) {
            tracedErrorIntrinsics.put(AttributeNames.ERROR_EXPECTED, true);
        } else {
            tracedErrorIntrinsics.put(AttributeNames.ERROR_EXPECTED, false);
        }

        // intrinsics go up even if attributes are off
        params.put("intrinsics", tracedErrorIntrinsics);

        // stack traces are not attributes - they get sent up regardless
        Collection<String> stackTrace = stackTrace();
        if (stackTrace != null) {
            params.put("stack_trace", stackTrace);
        } else {
            Map<String, Collection<String>> stackTraces = stackTraces();
            if (stackTraces != null) {
                params.put("stack_traces", stackTraces);
            }
        }

        return params;
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        // Wrapped in an ArrayList since Arrays.asList() returns an immutable list
        List<Object> elements = new ArrayList<>(Arrays.asList(getTimestampInMillis(), getPath(), getMessage(), getExceptionClass(), getAttributes()));
        // Add the transaction guid to the trace, if applicable
        if (transactionGuid != null) {
            elements.add(this.transactionGuid);
        }
        JSONArray.writeJSONString(elements, writer);
    }

    @Override
    public int compareTo(TracedError other) {
        return (int) (this.timestampInMillis - other.timestampInMillis);
    }

    public abstract boolean incrementsErrorMetric();

    public boolean isExpected() {
        return expected;
    }

}
