/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.OutboundHeaders;

public class NoOpSegment implements TracedActivity {

    public static final NoOpSegment INSTANCE = new NoOpSegment();

    private NoOpSegment() {
    }

    @Override
    public TracedMethod getTracedMethod() {
        return NoOpTracedMethod.INSTANCE;
    }

    @Override
    public void setAsyncThreadName(String threadName) {
    }

    @Override
    public void ignoreIfUnfinished() {
    }

    @Override
    public void finish() {
    }

    @Override
    public void finish(Throwable t) {
    }

    @Override
    public void setMetricName(String... metricNameParts) {
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
    }

    @Override
    public com.newrelic.api.agent.Transaction getTransaction() {
        return NoOpTransaction.INSTANCE;
    }

    @Override
    public void ignore() {
    }

    @Override
    public void end(){
    }

    @Override
    public void endAsync() {
    }

}
