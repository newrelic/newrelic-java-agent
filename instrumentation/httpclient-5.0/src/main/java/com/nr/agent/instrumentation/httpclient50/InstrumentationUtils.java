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
        NewRelic.getAgent().getLogger().log(Level.FINER, e, "Caught exception, checking for UnknownHost");
        if (e instanceof UnknownHostException) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build());
        }
    }

    public static void processResponse(URI requestURI, HttpResponse response, Segment segment) {
        if (response == null) {
            NewRelic.getAgent().getLogger().log(Level.INFO,"httpclient-5.0: null response received. No external recorded.");
            return;
        }
        HttpParameters params = createInboundParams(requestURI, response);
        if (segment != null) {
            segment.reportAsExternal(params);
        } else {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
    }

    public static void processResponse(URI requestURI, HttpResponse response) {
        processResponse(requestURI, response, null);
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
