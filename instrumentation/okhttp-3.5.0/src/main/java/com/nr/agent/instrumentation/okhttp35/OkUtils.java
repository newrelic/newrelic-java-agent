/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp35;

import java.net.URI;
import java.net.UnknownHostException;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;

import okhttp3.Request;
import okhttp3.Response;

public class OkUtils {

    private static final String LIBRARY = "OkHttp";

    private static final String PROCEDURE = "execute";

    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    /**
     * The original request is immutable, so internally the wrapper modifies a copy and saves it, which we need to
     * pull back out after adding the headers.
     */
    public static Request doOutboundCAT(Request request) {
        OutboundWrapper out = new OutboundWrapper(request);
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(out);
        return out.getRequestWithNRHeaders();
    }

    public static void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build());
        }
    }

    public static void processResponse(URI requestUri, Response response) {
        if (response != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(requestUri)
                    .procedure(PROCEDURE)
                    .inboundHeaders(new InboundWrapper(response))
                    .status(response.code(), response.message())
                    .build());
        }
    }

}
