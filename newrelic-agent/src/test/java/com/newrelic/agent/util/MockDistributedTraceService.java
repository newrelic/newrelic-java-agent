/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.util.Collections;
import java.util.Map;

public class MockDistributedTraceService extends DistributedTraceServiceImpl {
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getMajorSupportedCatVersion() {
        return 0;
    }

    @Override
    public int getMinorSupportedCatVersion() {
        return 2;
    }

    @Override
    public String getAccountId() {
        return "acct";
    }

    @Override
    public String getApplicationId() {
        return "42";
    }


    @Override
    public Map<String, Object> getIntrinsics(DistributedTracePayloadImpl inboundPayload, String guid, String traceId, TransportType transportType,
            long parentTransportDuration, long largestTransportDuration, String parentId, String parentSpanId, float priority) {
        return Collections.emptyMap();
    }

    @Override
    public String getTrustKey() {
        return "trustyrusty";
    }
}
