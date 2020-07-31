/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp30.internal;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.okhttp30.InboundWrapper;
import com.nr.agent.instrumentation.okhttp30.OutboundWrapper;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URI;
import java.net.UnknownHostException;

@Weave(type = MatchType.ExactClass, originalName = "okhttp3.RealCall")
abstract class RealCall_Instrumentation {

    @NewField
    private static final String LIBRARY = "OkHttp";

    @NewField
    private static final String PROCEDURE = "execute";

    @NewField
    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    /**
     * The original request is immutable, so internally the wrapper modifies a copy and saves it, which we need to
     * pull back out after adding the headers.
     */
    private static Request doOutboundCAT(Request request) {
        OutboundWrapper out = new OutboundWrapper(request);
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(out);
        return out.getRequestWithNRHeaders();
    }

    private static void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build());
        }
    }

    private static void processResponse(URI requestUri, Response response) {
        if (response != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(requestUri)
                    .procedure(PROCEDURE)
                    .inboundHeaders(new InboundWrapper(response))
                    .build());
        }
    }

    /**
     * Modifying the original request in the constructor might be better, but this is a work-around to a potential
     * problem in the agent accessing constructor parameters in any non-no-arg constructor.
     */
    @Trace
    public Response execute() {
        return Weaver.callOriginal();
    }

    @Trace
    public void enqueue(Callback responseCallback) {
        Weaver.callOriginal();
    }

    /**
     * Performs the request and returns the response. May return null if this call was canceled.
     * <p>
     * Both the sync and async paths go through this method.
     */
    @Trace(leaf = true)
    Response getResponse(Request request, boolean forWebSocket) throws Exception {
        request = doOutboundCAT(request);
        Response response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }

        processResponse(request.url().uri(), response);

        return response;
    }

    @Weave(type = MatchType.ExactClass, originalName = "okhttp3.RealCall$AsyncCall")
    abstract static class AsyncCall_Instrumentation {

        @NewField
        private Token token;

        private AsyncCall_Instrumentation(Callback responseCallback, boolean forWebSocket) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }

        @Trace(async = true)
        protected void execute() {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }

            Weaver.callOriginal();
        }

        @Trace(async = true)
        void cancel() {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }

            Weaver.callOriginal();
        }

    }

}
