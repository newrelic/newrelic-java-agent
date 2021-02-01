/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.grpc;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import io.grpc.Metadata;

import java.util.ArrayList;
import java.util.List;

public class InboundHeadersWrapper extends ExtendedInboundHeaders {

    private final Metadata metadata;
    private final Metadata trailers;

    public InboundHeadersWrapper(Metadata metadata, Metadata trailers) {
        this.metadata = metadata;
        this.trailers = trailers;
    }

    @Override
    public String getHeader(String key) {
        String value = null;
        if (metadata != null) {
            value = metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
        }
        // If we didn't find the value in the headers be sure to check the trailers as a fallback
        if (value == null && trailers != null) {
            value = trailers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
        }
        return value;
    }

    @Override
    public List<String> getHeaders(String name) {
        List<String> result = iterableToList(metadata.getAll(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER)));
        if ((result == null || result.isEmpty()) && trailers != null) {
            // If we didn't find the value in the headers be sure to check the trailers as a fallback
            result = iterableToList(trailers.getAll(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER)));
        }
        return result;
    }

    private List<String> iterableToList(Iterable<String> headers) {
        if (headers == null) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (String header : headers) {
            result.add(header);
        }
        return result;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

}
