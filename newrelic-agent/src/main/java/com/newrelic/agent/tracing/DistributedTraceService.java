/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase;

import java.util.Map;

public interface DistributedTraceService {

    boolean isEnabled();

    int getMajorSupportedCatVersion();

    int getMinorSupportedCatVersion();

    String getAccountId();

    String getApplicationId();

    float calculatePriority(Transaction tx, SamplerCase samplerCase);

    Map<String, Object> getIntrinsics(DistributedTracePayloadImpl inboundPayload, String guid, String traceId, TransportType transportType,
            long parentTransportDuration, long largestTransportDuration, String parentId, String parentSpanId, float priority);

    String getTrustKey();

    DistributedTracePayload createDistributedTracePayload(Tracer tracer);

}
