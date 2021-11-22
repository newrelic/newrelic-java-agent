/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.grpc;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.grpc.Metadata;
import io.grpc.internal.GrpcUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class GrpcRequest extends ExtendedRequest {

    private final String fullMethodName;
    private final String authority;
    private final Metadata metadata;

    public GrpcRequest(String fullMethodName, String authority, Metadata metadata) {
        this.fullMethodName = fullMethodName;
        this.authority = authority;
        this.metadata = metadata;
    }

    @Override
    public String getMethod() {
        return GrpcUtil.HTTP_METHOD;
    }

    @Override
    public String getRequestURI() {
        try {
            String path = "/" + fullMethodName;
            return new URI("grpc", authority, path, null, null).toASCIIString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return new String[0];
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return metadata.get(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER));
    }

    @Override
    public List<String> getHeaders(String name) {
        return iterableToList(metadata.getAll(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER)));
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
}
