/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ErrorEvent extends AnalyticsEvent implements JSONStreamAware {
    public static final float UNASSIGNED = Float.NEGATIVE_INFINITY;
    public static final int UNASSIGNED_INT = Integer.MIN_VALUE;
    public static final String TYPE = "TransactionError";
    public static final String UNKNOWN = "Unknown";

    /**
     * Required.
     */
    final String errorClass;

    /**
     * Required.
     */
    final String errorMessage;

    /**
     * Required.
     */
    final boolean errorExpected;

    /**
     * Required. Full metric name of the Transaction, or "Unknown" if not in a transaction.
     */
    final String transactionName;

    /**
     * Optional, requires Transaction. Duration of this transaction. Does not include queue time.
     */
    final float duration;

    /**
     * Optional, requires Transaction. Equivalent to 'WebFrontend/QueueTime' metric.
     */
    final float queueDuration;

    /**
     * Optional, requires Transaction. Equivalent to 'External/all' metric.
     */
    final float externalDuration;

    /**
     * Optional, requires Transaction. Equivalent to 'Datastore/all' metric.
     */
    final float databaseDuration;

    /**
     * Optional, requires Transaction. Equivalent to 'GC/cumulative' metric. Time spent in garbage collection across all
     * transactions during the timespan of this transaction.
     */
    final float gcCumulative;

    /**
     * Optional, requires Transaction. Equivalent to 'Datastore/all' call count.
     */
    final float databaseCallCount;

    /**
     * Optional, requires Transaction. Equivalent to 'External/all' call count.
     */
    final float externalCallCount;

    /**
     * Optional, requires Transaction
     */
    final String transactionGuid;

    /**
     * Optional, requires Transaction
     */
    final String referringTransactionGuid;

    /**
     * Optional, requires Transaction
     */
    final String syntheticsResourceId;

    /**
     * Optional, requires Transaction
     */
    final String syntheticsMonitorId;

    /**
     * Optional, requires Transaction
     */
    final String syntheticsJobId;

    /**
     * Optional, requires Transaction
     */
    final String syntheticsType;

    /**
     * Optional, requires Transaction
     */
    final String syntheticsInitiator;

    /**
     * Optional, requires Transaction
     */
    final Map<String, String> syntheticsAttributes;

    /**
     * Optional.
     */
    final int port;

    /**
     * Optional.
     */
    final String timeoutCause;

    /**
     * Better CAT trip ID
     */
    final String tripId;

    /**
     * Better CAT intrinsics
     */
    final Map<String, Object> distributedTraceIntrinsics;

    /**
     * Optional. Agent custom parameters. This includes request parameters.
     */
    final Map<String, Object> agentAttributes;

    /**
     * Required.
     */
    final String appName;
    private final AttributeFilter attributeFilter;

    public ErrorEvent(String appName, long timestamp, float priority, Map<String, Object> userAttributes,
                      String errorClass, String errorMessage, boolean errorExpected, String transactionName,
                      float duration, float queueDuration, float externalDuration, float databaseDuration,
                      float gcCumulative, float databaseCallCount, float externalCallCount, String transactionGuid,
                      String referringTransactionGuid, String syntheticsResourceId, String syntheticsMonitorId,
                      String syntheticsJobId, String syntheticsType, String syntheticsInitiator, Map<String, String> syntheticsAttributes, int port, String timeoutCause, String tripId, Map<String, Object>
                              distributedTraceIntrinsics, Map<String, Object> agentAttributes, AttributeFilter attributeFilter) {
        super(TYPE, timestamp, priority, new HashMap<>(userAttributes));
        this.errorClass = errorClass;
        this.errorMessage = errorMessage;
        this.errorExpected = errorExpected;
        this.transactionName = transactionName;
        this.duration = duration;
        this.queueDuration = queueDuration;
        this.externalDuration = externalDuration;
        this.databaseDuration = databaseDuration;
        this.gcCumulative = gcCumulative;
        this.databaseCallCount = databaseCallCount;
        this.externalCallCount = externalCallCount;
        this.transactionGuid = transactionGuid;
        this.referringTransactionGuid = referringTransactionGuid;
        this.syntheticsResourceId = syntheticsResourceId;
        this.syntheticsMonitorId = syntheticsMonitorId;
        this.syntheticsJobId = syntheticsJobId;
        this.syntheticsType = syntheticsType;
        this.syntheticsInitiator = syntheticsInitiator;
        this.syntheticsAttributes = syntheticsAttributes;
        this.port = port;
        this.timeoutCause = timeoutCause;
        this.tripId = tripId;
        this.distributedTraceIntrinsics = distributedTraceIntrinsics;
        this.agentAttributes = agentAttributes;
        this.appName = appName;
        this.attributeFilter = attributeFilter;
    }

    public String getErrorClass() {
        return errorClass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public Map<String, Object> getDistributedTraceIntrinsics() {
        return this.distributedTraceIntrinsics;
    }

    public String getTransactionGuid() {
        return transactionGuid;
    }


    /*
     * The data should go up as 3 hashes. Example: [ { "databaseDuration":value, "timestamp":value, "name":"value",
     * "duration":value, "type":"value" }, { "user_param1":"value", "user_param2":value, }, { "agent_param1": "value",
     * "agent_param2": value }
     */
    @SuppressWarnings("unchecked")
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("type", getType());
        obj.put("error.class", errorClass);
        obj.put("error.message", errorMessage);
        obj.put("timestamp", getTimestamp());
        obj.put("transactionName", transactionName);
        obj.put("error.expected", errorExpected);

        if (duration != UNASSIGNED) {
            obj.put("duration", duration);
        }
        if (queueDuration != UNASSIGNED) {
            obj.put("queueDuration", queueDuration);
        }
        if (externalDuration != UNASSIGNED) {
            obj.put("externalDuration", externalDuration);
        }
        if (databaseDuration > 0) {
            obj.put("databaseDuration", databaseDuration);
        }
        if (gcCumulative != UNASSIGNED) {
            obj.put("gcCumulative", gcCumulative);
        }
        if (databaseCallCount > 0) {
            obj.put("databaseCallCount", databaseCallCount);
        }
        if (externalCallCount > 0) {
            obj.put("externalCallCount", externalCallCount);
        }
        if (transactionGuid != null) {
            obj.put("nr.transactionGuid", transactionGuid); // prefixed to be hidden by Insights
        }
        if (referringTransactionGuid != null) {
            obj.put("nr.referringTransactionGuid", referringTransactionGuid); // prefixed to be hidden by Insights
        }
        if (this.syntheticsResourceId != null) {
            obj.put("nr.syntheticsResourceId", this.syntheticsResourceId);
        }
        if (this.syntheticsMonitorId != null) {
            obj.put("nr.syntheticsMonitorId", this.syntheticsMonitorId);
        }
        if (this.syntheticsJobId != null) {
            obj.put("nr.syntheticsJobId", this.syntheticsJobId);
        }
        if (this.syntheticsType != null) {
            obj.put("nr.syntheticsType", this.syntheticsType);
        }
        if (this.syntheticsInitiator != null) {
            obj.put("nr.syntheticsInitiator", this.syntheticsInitiator);
        }
        if (this.syntheticsAttributes != null) {
            String attrName, upperCaseKey;

            for (String key : this.syntheticsAttributes.keySet()) {
                upperCaseKey = Character.toUpperCase(key.charAt(0)) + key.substring(1);
                attrName = String.format("nr.synthetics%s", upperCaseKey);
                obj.put(attrName, this.syntheticsAttributes.get(key));
            }
        }
        if (port != UNASSIGNED_INT) {
            obj.put("port", port);
        }
        if (timeoutCause != null) {
            obj.put("nr.timeoutCause", timeoutCause);
        }
        if (distributedTraceIntrinsics != null && !distributedTraceIntrinsics.isEmpty()) {
            obj.putAll(distributedTraceIntrinsics);
        }
        if (tripId != null) {
            obj.put("nr.tripId", tripId); // prefixed to be hidden by Insights
        }
        if (getPriority() != UNASSIGNED) {
            obj.put("priority", getPriority());
        }

        Map<String, ?> filteredUserAttrs = attributeFilter.filterUserAttributes(appName, getMutableUserAttributes());
        Map<String, ?> filteredAgentAttrs = attributeFilter.filterAgentAttributes(appName, agentAttributes);
        if (filteredAgentAttrs.isEmpty()) {
            if (filteredUserAttrs.isEmpty()) {
                JSONArray.writeJSONString(Collections.singletonList(obj), out);
            } else {
                JSONArray.writeJSONString(Arrays.asList(obj, filteredUserAttrs), out);
            }
        } else {
            JSONArray.writeJSONString(Arrays.asList(obj, filteredUserAttrs, filteredAgentAttrs), out);
        }
    }

    @Override
    public boolean isValid() {
        // We don't need to validate the type "Error" every time.
        return true;
    }

    // VisibleForTesting
    public Map<String, Object> getAgentAttributes() {
        return Collections.unmodifiableMap(agentAttributes);
    }

}