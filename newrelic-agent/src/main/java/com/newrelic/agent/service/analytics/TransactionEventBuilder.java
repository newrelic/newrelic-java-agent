/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.model.CountedDuration;
import com.newrelic.agent.model.PathHashes;
import com.newrelic.agent.model.SyntheticsIds;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.model.TransactionTiming;
import com.newrelic.agent.model.SyntheticsInfo;

import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.service.analytics.TransactionEvent.UNASSIGNED_FLOAT;

public class TransactionEventBuilder {
    private String appName;
    private long timestamp;
    private String name;
    private float duration;
    private String guid;
    private String referringGuid;
    private Integer port;
    private String tripId;
    private ApdexPerfZone apdexPerfZone;
    private SyntheticsIds syntheticsIds;
    private SyntheticsInfo syntheticsInfo;
    private boolean error;
    private float pTotalTime;
    private TimeoutCause timeoutCause;
    private float priority;
    private final Map<String, Object> userAttributes = new HashMap<>();
    private Map<String, Object> distributedTraceIntrinsics;
    private float timeToFirstByte = UNASSIGNED_FLOAT;
    private float timeToLastByte = UNASSIGNED_FLOAT;
    private PathHashes pathHashes;
    private float queueDuration = UNASSIGNED_FLOAT;
    private float gcCumulative = UNASSIGNED_FLOAT;
    private CountedDuration external = CountedDuration.UNASSIGNED;
    private CountedDuration database = CountedDuration.UNASSIGNED;

    public TransactionEventBuilder() {
    }

    public TransactionEventBuilder setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public TransactionEventBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public TransactionEventBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public TransactionEventBuilder setDuration(float duration) {
        this.duration = duration;
        return this;
    }

    public TransactionEventBuilder setGuid(String guid) {
        this.guid = guid;
        return this;
    }

    public TransactionEventBuilder setReferringGuid(String referringGuid) {
        this.referringGuid = referringGuid;
        return this;
    }

    public TransactionEventBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    public TransactionEventBuilder setTripId(String tripId) {
        this.tripId = tripId;
        return this;
    }

    public TransactionEventBuilder setApdexPerfZone(ApdexPerfZone apdexPerfZone) {
        this.apdexPerfZone = apdexPerfZone;
        return this;
    }

    public TransactionEventBuilder setError(boolean error) {
        this.error = error;
        return this;
    }

    public TransactionEventBuilder setpTotalTime(float pTotalTime) {
        this.pTotalTime = pTotalTime;
        return this;
    }

    public TransactionEventBuilder setTimeoutCause(TimeoutCause timeoutCause) {
        this.timeoutCause = timeoutCause;
        return this;
    }

    public TransactionEventBuilder setPriority(float priority) {
        this.priority = priority;
        return this;
    }

    public TransactionEventBuilder putAllUserAttributes(Map<String, Object> additionalAttributes) {
        this.userAttributes.putAll(additionalAttributes);
        return this;
    }

    public TransactionEventBuilder setDistributedTraceIntrinsics(Map<String, Object> distributedTraceIntrinsics) {
        this.distributedTraceIntrinsics = distributedTraceIntrinsics;
        return this;
    }

    public TransactionEventBuilder setTimeToFirstByte(float timeToFirstByte) {
        this.timeToFirstByte = timeToFirstByte;
        return this;
    }

    public TransactionEventBuilder setTimeToLastByte(float timeToLastByte) {
        this.timeToLastByte = timeToLastByte;
        return this;
    }

    public TransactionEventBuilder setSyntheticsIds(SyntheticsIds syntheticsIds) {
        this.syntheticsIds = syntheticsIds;
        return this;
    }

    public TransactionEventBuilder setSyntheticsInfo(SyntheticsInfo syntheticsInfo) {
        this.syntheticsInfo = syntheticsInfo;
        return this;
    }

    public TransactionEventBuilder setPathHashes(PathHashes pathHashes) {
        this.pathHashes = pathHashes;
        return this;
    }

    public TransactionEventBuilder setQueueDuration(float queueDuration) {
        this.queueDuration = queueDuration;
        return this;
    }

    public TransactionEventBuilder setExternal(CountedDuration external) {
        this.external = external;
        return this;
    }

    public TransactionEventBuilder setDatabase(CountedDuration database) {
        this.database = database;
        return this;
    }

    public TransactionEventBuilder setGcCumulative(float gcCumulative) {
        this.gcCumulative = gcCumulative;
        return this;
    }

    public TransactionEvent build() {
        if (syntheticsIds == null) {
            syntheticsIds = new SyntheticsIds(null, null, null);
        }
        if (syntheticsInfo == null) {
            syntheticsInfo = new SyntheticsInfo(null, null, null);
        }
        if (pathHashes == null) {
            pathHashes = new PathHashes(null, null, null);
        }
        TransactionTiming timing = TransactionTiming.builder()
                .duration(duration)
                .totalTime(pTotalTime)
                .timeToFirstByte(timeToFirstByte)
                .timeToLastByte(timeToLastByte)
                .queueDuration(queueDuration)
                .external(external)
                .database(database)
                .gcCumulative(gcCumulative)
                .build();

        return new TransactionEvent(appName, userAttributes, timestamp, name, timing, guid, referringGuid, port, tripId,
                pathHashes, apdexPerfZone, syntheticsIds, syntheticsInfo, error, timeoutCause,
                priority, distributedTraceIntrinsics);
    }
}