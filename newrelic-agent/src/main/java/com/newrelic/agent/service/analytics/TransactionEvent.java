/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.model.PathHashes;
import com.newrelic.agent.model.SyntheticsIds;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.model.TransactionTiming;
import com.newrelic.agent.model.SyntheticsInfo;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransactionEvent extends AnalyticsEvent implements JSONStreamAware {

    static final float UNASSIGNED_FLOAT = Float.NEGATIVE_INFINITY;
    static final int UNASSIGNED_INT = Integer.MIN_VALUE;

    static final String TYPE = "Transaction";

    private final String appName;
    private final String guid;
    private final String referrerGuid;
    private final String tripId;
    private final PathHashes pathHashes;

    private final ApdexPerfZone apdexPerfZone;
    private final SyntheticsIds syntheticsIds;
    private final SyntheticsInfo syntheticsInfo;
    private final TransactionTiming timing;

    private final int port;
    private final boolean error;
    private final boolean decider;
    /**
     * Required. Full metric name of the transaction
     */
    private final String name;

    /**
     * Transaction timeout cause.
     * https://pages.datanerd.us/engineering-management/architecture-notes/notes/123/#nrtimeoutCause
     */
    private final TimeoutCause timeoutCause;
    private final Map<String, Object> distributedTraceIntrinsics;

    /**
     * Optional. Agent custom parameters. This includes request parameters.
     */
    Map<String, Object> agentAttributes;



    public TransactionEvent(String appName, Map<String, Object> userAttributes, long timestamp, String name, TransactionTiming timing,
                            String guid, String referringGuid, Integer port, String tripId, PathHashes pathHashes,
                            ApdexPerfZone apdexPerfZone, SyntheticsIds syntheticsIds, SyntheticsInfo syntheticsInfo, boolean error, TimeoutCause timeoutCause,
                            float priority, Map<String, Object> distributedTraceIntrinsics, boolean decider) {
        super(TYPE, timestamp, priority, userAttributes);
        if (pathHashes == null) throw new NullPointerException("pathHashes must not be null");
        if (syntheticsIds == null) throw new NullPointerException("syntheticsIds must not be null");
        if (syntheticsInfo == null) throw new NullPointerException("syntheticsInfo must not be null");
        if (timing == null) throw new NullPointerException("timing must not be null");
        this.name = name;
        this.timing = timing;
        this.guid = guid;
        this.referrerGuid = referringGuid;
        this.tripId = tripId;
        this.pathHashes = pathHashes;
        this.port = port == null ? UNASSIGNED_INT : port;
        this.appName = appName;
        this.apdexPerfZone = apdexPerfZone;
        this.syntheticsIds = syntheticsIds;
        this.syntheticsInfo = syntheticsInfo;
        this.error = error;
        this.timeoutCause = timeoutCause;
        this.decider = decider;
        this.distributedTraceIntrinsics = distributedTraceIntrinsics;
    }

    public float getDuration() {
        return timing.getDuration();
    }

    public float getTotalTime() {
        return timing.getTotalTime();
    }

    public float getTTFB() {
        return timing.getTimeToFirstByte();
    }

    public float getTTLB() {
        return timing.getTimeToLastByte();
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public float getExternalCallCount() {
        return timing.getExternalCallCount();
    }

    public float getExternalDuration() {
        return timing.getExternalDuration();
    }

    public float getDatabaseCallCount() {
        return timing.getDatabaseCallCount();
    }

    public float getDatabaseDuration() {
        return timing.getDatabaseDuration();
    }

    public boolean isError() {
        return error;
    }

    public String getGuid() {
        return guid;
    }

    public String getTripId() {
        return tripId;
    }

    public Integer getPathHash() {
        return pathHashes.getPathHash();
    }

    public String getAlternatePathHashes() {
        return pathHashes.getAlternatePathHashes();
    }

    public Integer getReferringPathHash() {
        return pathHashes.getReferringPathHash();
    }

    public String getReferrerGuid() {
        return referrerGuid;
    }

    public String getApdexPerfZone() {
        if (apdexPerfZone != null) {
            return apdexPerfZone.getZone();
        }
        return null;
    }

    public TimeoutCause getTimeoutCause() {
        return timeoutCause;
    }

    @VisibleForTesting
    public Map<String, Object> getDistributedTraceIntrinsics() {
        return distributedTraceIntrinsics;
    }

    @VisibleForTesting
    public String getParentId() {
        return distributedTraceIntrinsics == null ? null : (String) distributedTraceIntrinsics.get("parentId");
    }

    @VisibleForTesting
    public String getParenSpanId() {
        return distributedTraceIntrinsics == null ? null : (String) distributedTraceIntrinsics.get("parentSpanId");
    }

    /*
     * The data should go up as 3 hashes. Example: [ { "webDuration":value, "databaseDuration":value, "timestamp":value,
     * "name":"value", "duration":value, "type":"value" }, { "user_param1":"value", "user_param2":value, }, {
     * "agent_param1": "value", "agent_param2": value }
     */

    @SuppressWarnings("unchecked")
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("type", getType());
        obj.put("timestamp", getTimestamp());
        obj.put("name", name);
        obj.put("duration", timing.getDuration());
        obj.put("error", error);
        obj.put("totalTime", timing.getTotalTime());
        obj.put("priority", getPriority());

        if (timing.getTimeToFirstByte() != UNASSIGNED_FLOAT) {
            obj.put("timeToFirstByte", timing.getTimeToFirstByte());
        }
        if (timing.getTimeToLastByte() != UNASSIGNED_FLOAT) {
            obj.put("timeToLastByte", timing.getTimeToLastByte());
        }
        if (apdexPerfZone != null) {
            obj.put("apdexPerfZone", apdexPerfZone.getZone());
        }

        DistributedTracingConfig distributedTracingConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig();
        if (!distributedTracingConfig.isEnabled()) {
            if (tripId != null) {
                obj.put("nr.tripId", tripId); // prefixed to be hidden by Insights
            }
            if (guid != null) {
                obj.put("nr.guid", guid); // prefixed to be hidden by Insights
            }
            if (getPathHash() != null) {
                // properly handles leading 0's
                obj.put("nr.pathHash", String.format("%08x", getPathHash())); // prefixed to be hidden by Insights
            }
            if (getReferringPathHash() != null) {
                // properly handles leading 0's
                obj.put("nr.referringPathHash", String.format("%08x", getReferringPathHash()));
            }
            if (getAlternatePathHashes() != null) {
                obj.put("nr.alternatePathHashes", getAlternatePathHashes()); // prefixed to be hidden by Insights
            }
            if (referrerGuid != null) {
                obj.put("nr.referringTransactionGuid", referrerGuid); // prefixed to be hidden by Insights
            }
        }
        if (this.syntheticsIds.getResourceId() != null) {
            obj.put("nr.syntheticsResourceId", this.syntheticsIds.getResourceId());
        }
        if (this.syntheticsIds.getMonitorId() != null) {
            obj.put("nr.syntheticsMonitorId", this.syntheticsIds.getMonitorId());
        }
        if (this.syntheticsIds.getJobId() != null) {
            obj.put("nr.syntheticsJobId", this.syntheticsIds.getJobId());
        }
        if (this.syntheticsInfo.getType() != null) {
            obj.put("nr.syntheticsType", this.syntheticsInfo.getType());
        }
        if (this.syntheticsInfo.getInitiator() != null) {
            obj.put("nr.syntheticsInitiator", this.syntheticsInfo.getInitiator());
        }
        if (this.syntheticsInfo.getAttributeMap() != null) {
            Map<String, String> attrMap = this.syntheticsInfo.getAttributeMap();
            String attrName, upperCaseKey;

            for (String key : attrMap.keySet()) {
                upperCaseKey = Character.toUpperCase(key.charAt(0)) + key.substring(1);
                attrName = String.format("nr.synthetics%s", upperCaseKey);
                obj.put(attrName, attrMap.get(key));
            }
        }
        if (port != UNASSIGNED_INT) {
            obj.put("port", port);
        }
        if (timing.getQueueDuration() != UNASSIGNED_FLOAT) {
            obj.put("queueDuration", timing.getQueueDuration());
        }
        if (getExternalDuration() != UNASSIGNED_FLOAT) {
            obj.put("externalDuration", getExternalDuration());
        }
        if (getExternalCallCount() > 0) {
            obj.put("externalCallCount", getExternalCallCount());
        }
        if (getDatabaseDuration() != UNASSIGNED_FLOAT) {
            obj.put("databaseDuration", getDatabaseDuration());
        }
        if (getDatabaseCallCount() > 0) {
            obj.put("databaseCallCount", getDatabaseCallCount());
        }
        if (timing.getGcCumulative() != UNASSIGNED_FLOAT) {
            obj.put("gcCumulative", timing.getGcCumulative());
        }
        if (timeoutCause != null) {
            obj.put("nr.timeoutCause", timeoutCause.cause);
        }

        if (distributedTraceIntrinsics != null && !distributedTraceIntrinsics.isEmpty()) {
            obj.putAll(distributedTraceIntrinsics);
        }

        Map<String, ?> filteredUserAtts = getUserFilteredMap(getMutableUserAttributes());
        Map<String, ?> filteredAgentAtts = getFilteredMap(agentAttributes);
        if (filteredAgentAtts.isEmpty()) {
            if (filteredUserAtts.isEmpty()) {
                JSONArray.writeJSONString(Collections.singletonList(obj), out);
            } else {
                JSONArray.writeJSONString(Arrays.asList(obj, filteredUserAtts), out);
            }
        } else {
            JSONArray.writeJSONString(Arrays.asList(obj, filteredUserAtts, filteredAgentAtts), out);
        }
    }
    private Map<String, ?> getFilteredMap(Map<String, ?> input) {
        return ServiceFactory.getAttributesService().filterTransactionEventAttributes(appName, input);
    }

    private Map<String, ?> getUserFilteredMap(Map<String, ?> input) {
        // user attributes should have already been filtered for high security - this is just extra protection
        // high security is per an account - meaning it can not be different for various application names within a
        // JVM - so we can just check the default agent config
        if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
            return getFilteredMap(input);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean isValid() {
        // We don't need to validate the type "Transaction" every time.
        return true;
    }

    @Override
    public boolean decider() {
        return decider;
    }

    @VisibleForTesting
    public Map<String, Object> getAgentAttributesCopy() {
        return new HashMap<>(agentAttributes);
    }

    public String getSyntheticsJobId() {
        return syntheticsIds.getJobId();
    }
}
