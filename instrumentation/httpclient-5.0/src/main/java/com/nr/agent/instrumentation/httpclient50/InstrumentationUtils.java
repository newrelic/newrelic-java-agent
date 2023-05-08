/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class InstrumentationUtils {

    public static final String LIBRARY = "CommonsHttp";
    public static final String PROCEDURE = "execute";
    public static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    public static Segment startAsyncSegment() {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        return txn == null ? null : txn.startSegment("HttpAsyncClient.execute");
    }

    public static void doOutboundCAT(HttpRequest request, Segment segment) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "inside doOutboundCAT");
        OutboundHeaders outboundHeaders = new OutboundWrapper(request);
        if (segment != null) {
            segment.addOutboundRequestHeaders(outboundHeaders);
        } else {
            NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(outboundHeaders);
        }

    }

    public static void doOutboundCAT(HttpRequest request) {
        doOutboundCAT(request, null);
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

    public static void processResponse(URI requestURI, HttpResponse response, Segment segment) {
        HttpParameters params = createInboundParams(requestURI, response);
        if (segment != null) {
            segment.reportAsExternal(params);
        } else {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
    }

    public static void processResponse(URI requestURI, HttpResponse response, TracedMethod tracedMethod) {
        HttpParameters params = createInboundParams(requestURI, response);
        if (tracedMethod != null) {
            tracedMethod.reportAsExternal(params);
        } else {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
    }

    private static  HttpParameters createInboundParams(URI requestURI, HttpResponse response) {
        InboundWrapper inboundCatWrapper = new InboundWrapper(response);
        HttpParameters params = HttpParameters
                .library(LIBRARY)
                .uri(requestURI)
                .procedure(PROCEDURE)
                .inboundHeaders(inboundCatWrapper)
                .status(response.getCode(), response.getReasonPhrase())
                .build();
        return params;
    }

}
