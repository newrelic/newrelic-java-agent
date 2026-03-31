/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;

import java.net.URI;
import java.net.UnknownHostException;

public class RestTemplateUtils {

    public static final String LIBRARY = "RestTemplate";
    public static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    public static void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException || (e.getCause() instanceof UnknownHostException)) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure("execute")
                    .build());
        }
    }

    public static <T> void processResponse(URI uri, HttpMethod method, T result) {
        if (uri == null) {
            return;
        }

        String procedure = method != null ? method.name() : "execute";

        InboundHeadersWrapper inboundHeaders = extractInboundHeaders(result);

        if (inboundHeaders != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(uri)
                    .procedure(procedure)
                    .inboundHeaders(inboundHeaders)
                    .build());
        } else {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(uri)
                    .procedure(procedure)
                    .noInboundHeaders()
                    .build());
        }
    }

    private static <T> InboundHeadersWrapper extractInboundHeaders(T result) {
        if (result == null) {
            return null;
        }

        if (result instanceof ResponseEntity) {
            return new InboundHeadersWrapper(((ResponseEntity<?>) result).getHeaders());
        }

        if (result instanceof ClientHttpResponse) {
            return new InboundHeadersWrapper(((ClientHttpResponse) result).getHeaders());
        }

        return null;
    }
}