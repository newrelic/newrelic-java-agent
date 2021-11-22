/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.grpc;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import io.grpc.Metadata;
import io.grpc.Status;

public class GrpcResponse extends ExtendedResponse {

    private final Status status;
    private final Metadata headers;

    public GrpcResponse(Status status, Metadata headers) {
        this.status = status;
        this.headers = headers;
    }

    @Override
    public long getContentLength() {
        // There isn't a good value here for gRPC
        return -1;
    }

    @Override
    public int getStatus() throws Exception {
        return status.getCode().value();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return status.getDescription();
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        if (GrpcConfig.disributedTracingEnabled) {
            headers.put(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER), value);
        }
    }

}
