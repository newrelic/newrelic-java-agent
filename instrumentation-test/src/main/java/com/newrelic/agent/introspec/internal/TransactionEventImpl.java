/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.TransactionEvent;

import java.util.HashMap;
import java.util.Map;

class TransactionEventImpl extends EventImpl implements TransactionEvent {

    private float duration;
    private float totalTime;
    private int externalCallCount;
    private float externalDuration;
    private int databaseCallCount;
    private float databaseDuration;
    private boolean isError;
    private int port;
    private String name;
    private String myGuid;
    private String tripId;
    private String parentId;
    private String parentSpanId;
    private Integer myPathHash;
    private String referrerGuid;
    private Integer referringPathHash;
    private String apdexPerfZone;
    private String alternatePathHashs;
    private Map<String, Object> distributedTraceIntrinsics;

    public TransactionEventImpl(com.newrelic.agent.service.analytics.TransactionEvent event) {
        super(event.getType(), new HashMap<String, Object>());

        getAttributes().putAll(event.getUserAttributesCopy());
        getAttributes().putAll(event.getAgentAttributesCopy());

        this.duration = event.getDuration();
        this.totalTime = event.getTotalTime();

        if (event.getExternalCallCount() % 1 != 0) {
            throw new IllegalArgumentException("External call count should always be an integer");
        }
        this.externalCallCount = (int) event.getExternalCallCount();
        this.externalDuration = event.getExternalDuration();

        if (event.getDatabaseCallCount() % 1 != 0) {
            throw new IllegalArgumentException("Database call count should always be an integer");
        }
        this.databaseCallCount = (int) event.getDatabaseCallCount();
        this.databaseDuration = event.getDatabaseDuration();

        this.port = event.getPort();
        this.name = event.getName();
        this.isError = event.isError();
        this.myGuid = event.getGuid();
        this.tripId = event.getTripId();
        this.myPathHash = event.getPathHash();
        this.referrerGuid = event.getReferrerGuid();
        this.referringPathHash = event.getReferringPathHash();
        this.apdexPerfZone = event.getApdexPerfZone();
        this.alternatePathHashs = event.getAlternatePathHashes();
        this.parentId = event.getParentId();
        this.parentSpanId = event.getParenSpanId();
        this.distributedTraceIntrinsics = event.getDistributedTraceIntrinsics();
    }

    @Override
    public float getDurationInSec() {
        return duration;
    }

    @Override
    public float getTotalTimeInSec() {
        return totalTime;
    }

    @Override
    public int getExternalCallCount() {
        return externalCallCount;
    }

    @Override
    public float getExternalDurationInSec() {
        return externalDuration;
    }

    @Override
    public int getDatabaseCallCount() {
        return databaseCallCount;
    }

    @Override
    public float getDatabaseDurationInSec() {
        return databaseDuration;
    }

    @Override
    public boolean isError() {
        return isError;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTripId() {
        return tripId;
    }

    public String getMyGuid() {
        return myGuid;
    }

    public Integer getMyPathHash() {
        return myPathHash;
    }

    public String getReferrerGuid() {
        return referrerGuid;
    }

    public Integer getReferringPathHash() {
        return referringPathHash;
    }

    public String getApdexPerfZone() {
        return apdexPerfZone;
    }

    public String getMyAlternatePathHashes() {
        return alternatePathHashs;
    }

    @Override
    public String getParentType() {
        return (String) distributedTraceIntrinsics.get("parent.type");
    }

    @Override
    public String getParentApplicationId() {
        return (String) distributedTraceIntrinsics.get("parent.app");
    }

    @Override
    public String getParentAccountId() {
        return (String) distributedTraceIntrinsics.get("parent.account");
    }

    @Override
    public String getParentTransportType() {
        return (String) distributedTraceIntrinsics.get("parent.transportType");
    }

    @Override
    public Float getParentTransportDuration() {
        return (Float) distributedTraceIntrinsics.get("parent.transportDuration");
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public Float priority() {
        return (Float) distributedTraceIntrinsics.get("priority");
    }

    @Override
    public Boolean sampled() {
        return (Boolean) distributedTraceIntrinsics.get("sampled");
    }

    @Override
    public String getParentSpanId() {
        return parentSpanId;
    }
}
