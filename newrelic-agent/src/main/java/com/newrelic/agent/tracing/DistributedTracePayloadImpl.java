/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.DistributedTracePayload;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

import static com.newrelic.agent.tracing.DistributedTraceUtil.*;

public class DistributedTracePayloadImpl implements DistributedTracePayload {

    public final long timestamp;
    public final String parentType;
    public final String accountId;
    public final String trustKey;
    public final String applicationId;
    public final String guid;
    public final String traceId;
    public final Float priority;
    public final Sampled sampled;
    public final String txnId;

    public static DistributedTracePayloadImpl createDistributedTracePayload(String traceId, String guid,
            String txnId, float priority) {
        DistributedTraceService distributedTraceService = ServiceFactory.getDistributedTraceService();
        String accountId = distributedTraceService.getAccountId();
        if (accountId == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, "Not creating distributed trace payload due to null accountId."
                    + " Make sure the agent has connected to New Relic.");
            return null;
        }

        String trustKey = distributedTraceService.getTrustKey();
        String applicationId = distributedTraceService.getApplicationId();
        long timestamp = System.currentTimeMillis();
        Sampled sampled = DistributedTraceUtil.isSampledPriority(priority) ? Sampled.SAMPLED_YES : Sampled.SAMPLED_NO;
        return new DistributedTracePayloadImpl(timestamp, APP_PARENT_TYPE, accountId, trustKey, applicationId, guid, traceId, txnId, priority, sampled);
    }

    protected DistributedTracePayloadImpl(long timestamp, String parentType, String accountId,
        String trustKey, String applicationId,
        String guid, String traceId, String txnId, Float priority, Sampled sampled){
        this.timestamp = timestamp;
        this.parentType = parentType;
        this.accountId = accountId;
        this.trustKey = trustKey;
        this.applicationId = applicationId;
        this.guid = guid;
        this.txnId = txnId;
        this.traceId = traceId;
        this.priority = priority;
        this.sampled = sampled;
    }

    public String getGuid() {
        return guid;
    }

    public String getTransactionId() { return txnId; }

    @Override
    @SuppressWarnings("unchecked")
    public String text() {
        DistributedTraceService distributedTraceService = ServiceFactory.getDistributedTraceService();

        JSONObject payload = new JSONObject();

        JSONArray catVersion = new JSONArray();
        catVersion.add(distributedTraceService.getMajorSupportedCatVersion());
        catVersion.add(distributedTraceService.getMinorSupportedCatVersion());
        payload.put(VERSION, catVersion); // version [major, minor]

        JSONObject data = new JSONObject();

        data.put(TIMESTAMP, timestamp);
        data.put(PARENT_TYPE, parentType);
        data.put(ACCOUNT_ID, accountId);

        if (!accountId.equals(trustKey)) {
            data.put(TRUSTED_ACCOUNT_KEY, trustKey);
        }

        data.put(APPLICATION_ID, applicationId);

        if (guid != null) {
            // span events is enabled
            data.put(GUID, guid);
        }

        data.put(TRACE_ID, traceId);
        data.put(PRIORITY, priority);
        data.put(SAMPLED, sampled.booleanValue());

        if (txnId != null) {
            data.put(TX, txnId);
        }

        payload.put(DATA, data);
        return payload.toJSONString();
    }

    @Override
    public String httpSafe() {
        return Base64.getEncoder().encodeToString(text().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "DistributedTracePayloadImpl{" +
                "timestamp=" + timestamp +
                ", parentType='" + parentType + '\'' +
                ", accountId='" + accountId + '\'' +
                ", trustKey='" + trustKey + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", guid='" + guid + '\'' +
                ", traceId='" + traceId + '\'' +
                ", txnId='" + txnId + '\'' +
                ", sampled=" + sampled +
                ", priority=" + priority +
                '}';
    }
}
