/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class W3CTraceState {

    private final List<String> traceStateHeaders;
    private final boolean containsNrData;
    private final List<String> vendorStates;
    private final int version;
    private final String trustKey;
    private final ParentType parentType;
    private final String guid;
    private final String accountId;
    private final String applicationId;
    private final String txnId;
    private final Sampled sampled;
    private final Float priority;
    private final long timestamp;

    W3CTraceState(List<String> traceStateHeaders, List<String> vendorStates) {
        this(traceStateHeaders, vendorStates, false, W3CTraceStateSupport.NR_HEADER_VERSION_INT, null,
                ParentType.Invalid, null, null, null, null, Sampled.UNKNOWN, null, 0);
    }

    W3CTraceState(List<String> traceStateHeaders, List<String> vendorStates, boolean containsNrData, int version, String trustKey, ParentType parentType,
            String accountId, String applicationId, String guid, String txnId, Sampled sampled, Float priority, long timestamp) {
        this.traceStateHeaders = new ArrayList<>(traceStateHeaders);
        this.containsNrData = containsNrData;
        this.vendorStates = new ArrayList<>(vendorStates);
        this.version = version;
        this.trustKey = trustKey;
        this.parentType = parentType;
        this.accountId = accountId;
        this.applicationId = applicationId;
        this.txnId = txnId;
        this.guid = guid;
        this.sampled = sampled;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    public List<String> getTraceStateHeaders() {
        return traceStateHeaders;
    }

    boolean containsNrData() {
        return containsNrData;
    }

    public List<String> getVendorStates() {
        return Collections.unmodifiableList(vendorStates);
    }

    public int getVersion() {
        return version;
    }

    public String getTrustKey() {
        return trustKey;
    }

    public ParentType getParentType() {
        return parentType;
    }

    public String getGuid() {
        return guid;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getTxnId() {
        return txnId;
    }

    public Sampled getSampled() {
        return sampled;
    }

    public Float getPriority() {
        return priority;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        W3CTraceState that = (W3CTraceState) o;
        return containsNrData == that.containsNrData &&
                version == that.version &&
                timestamp == that.timestamp &&
                Objects.equals(traceStateHeaders, that.traceStateHeaders) &&
                Objects.equals(vendorStates, that.vendorStates) &&
                Objects.equals(trustKey, that.trustKey) &&
                Objects.equals(parentType, that.parentType) &&
                Objects.equals(guid, that.guid) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(txnId, that.txnId) &&
                Objects.equals(sampled, that.sampled) &&
                Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceStateHeaders, containsNrData, vendorStates, version, trustKey, parentType, guid, accountId, applicationId, txnId, sampled,
                priority, timestamp);
    }
}
