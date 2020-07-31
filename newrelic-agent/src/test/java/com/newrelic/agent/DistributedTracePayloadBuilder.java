/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class DistributedTracePayloadBuilder {

    private long timestamp;
    private int minorVersion = 0;
    private int majorVersion = 2;
    private String host;
    private String parentType;
    private String accountId;
    private String trustKey;
    private String applicationId;
    private String transactionId;
    private String tripId;
    private Float priority;
    private int depth;
    private String syntheticsResource;
    private String syntheticsJob;
    private String syntheticsMonitor;

    public DistributedTracePayloadBuilder() {

    }

    public DistributedTracePayloadBuilder setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
        return this;
    }

    public DistributedTracePayloadBuilder setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        return this;
    }

    public DistributedTracePayloadBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public DistributedTracePayloadBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public DistributedTracePayloadBuilder setParentType(String parentType) {
        this.parentType = parentType;
        return this;
    }

    public DistributedTracePayloadBuilder setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public DistributedTracePayloadBuilder setTrustKey(String trustKey) {
        if (trustKey != null) {
            this.trustKey = trustKey;
            return this;
        }
        this.trustKey = this.accountId;
        return this;
    }

    public DistributedTracePayloadBuilder setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public DistributedTracePayloadBuilder setTransactionId(String parentApplicationId) {
        this.transactionId = transactionId;
        return this;
    }

    public DistributedTracePayloadBuilder setTripId(String tripId) {
        this.tripId = tripId;
        return this;
    }

    public DistributedTracePayloadBuilder setPriority(Float priority) {
        this.priority = priority;
        return this;
    }

    public DistributedTracePayloadBuilder setDepth(int depth) {
        this.depth = depth;
        return this;
    }

    public DistributedTracePayloadBuilder setSyntheticsResource(String syntheticsResource) {
        this.syntheticsResource = syntheticsResource;
        return this;
    }

    public DistributedTracePayloadBuilder setSyntheticsJob(String syntheticsJob) {
        this.syntheticsJob = syntheticsJob;
        return this;
    }

    public DistributedTracePayloadBuilder setSyntheticsMonitor(String syntheticsMonitor) {
        this.syntheticsMonitor = syntheticsMonitor;
        return this;
    }

    public String createJsonPayload() {
        String syntheticsData = "";
        if (syntheticsJob != null && syntheticsMonitor != null && syntheticsResource != null) {
            syntheticsData = " \"sy\": {" +
                "      \"r\": \"" + syntheticsResource + "\"," +
                "      \"j\": \"" + syntheticsJob + "\"," +
                "      \"m\": \"" + syntheticsMonitor +"\"" +
                "    }" ;
        }

        return "{" +
                "  \"v\": [" + minorVersion + "," + majorVersion + "]," +
                "  \"d\": {" +
                "    \"ty\": \"" + parentType + "\"," +
                "    \"ac\": \"" + accountId + "\"," +
                "    \"tk\": \"" + trustKey + "\"," +
                "    \"ap\": \"" + applicationId + "\"" +
                "    \"id\": \"" + transactionId + "\"," +
                "    \"tr\": \"" + tripId + "\"," +
                "    \"de\": " + depth + "," +
                "    \"pr\": " + priority + "," +
                "    \"ti\": " + timestamp + "," +
                "    \"ho\": \"" + host + "\"," +
                syntheticsData +
                "  }" +
                "}";
    }

}
