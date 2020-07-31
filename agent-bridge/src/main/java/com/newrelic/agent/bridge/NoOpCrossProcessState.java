/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

import java.net.URI;

public class NoOpCrossProcessState implements CrossProcessState {
    public static final CrossProcessState INSTANCE = new NoOpCrossProcessState();

    @Override
    public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders) {

    }

    @Override
    public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders,
                                              com.newrelic.api.agent.TracedMethod tracedMethod) {
    }

    @Override
    public void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength) {
    }

    @Override
    public String getRequestMetadata() {
        return null;
    }

    @Override
    public void processRequestMetadata(String requestMetadata) {
    }

    @Override
    public String getResponseMetadata() {
        return null;
    }

    @Override
    public void processResponseMetadata(String responseMetadata, URI uri) {
    }

    @Override
    public void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host,
            String uri, boolean addRollupMetric) {
    }

}
