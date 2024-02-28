/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.grpc;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import io.grpc.Metadata;

public class OutboundHeadersWrapper implements OutboundHeaders {

    private final Metadata metadata;

    public OutboundHeadersWrapper(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String key, String value) {
        if (GrpcConfig.disributedTracingEnabled) {
            if (!metadata.containsKey(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER))) {
                metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);
            }
        }
    }
}
