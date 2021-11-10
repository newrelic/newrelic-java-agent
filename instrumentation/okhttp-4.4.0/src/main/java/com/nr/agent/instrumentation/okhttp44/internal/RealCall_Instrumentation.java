/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp44.internal;

import com.newrelic.agent.bridge.jfr.events.external.HttpExternalEvent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import com.nr.agent.instrumentation.okhttp44.OkUtils;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

@Weave(type = MatchType.ExactClass, originalName = "okhttp3.internal.connection.RealCall")
public abstract class RealCall_Instrumentation {
    @NewField
    private HttpExternalEvent httpExternalEvent; // fixme adding this object could be problematic if jfr apis don't exist

    public Request getOriginalRequest() {
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Response execute() {
        beginJfrEvent();
        return Weaver.callOriginal();
    }

    @Trace
    public void enqueue(Callback responseCallback) {
        Weaver.callOriginal();
    }

    /**
     * Both the sync and async paths go through this method.
     *
     * Wrapping the originalRequest again is hacky, since it was previously wrapped in Factory#newCall, but it needs
     * to happen in the same traced method as processing the response. And it can't only happen here, since the
     * reference is final and the code executing in Weaver.callOriginal needs to operate on the wrapped version.
     */
    public Response getResponseWithInterceptorChain$okhttp() throws Exception {
        Response response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            OkUtils.handleUnknownHost(e);
            endJfrEvent(); // end the event in case of exception during request execution?
            throw e;
        }

        OkUtils.processResponse(getOriginalRequest().url().uri(), response);

        Request request = response != null ? response.request() : null;
        endJfrEvent();
        // TODO ideally some condition check for shouldCommit()
        commitJfrEvent(request, response);

        return response;
    }

    private void beginJfrEvent() {
        httpExternalEvent = new HttpExternalEvent();
        httpExternalEvent.begin();
    }

    private void endJfrEvent() {
        httpExternalEvent.end();
    }

    private void commitJfrEvent(Request request, Response response) throws IOException {
        httpExternalEvent.httpClient = "OkHttp";
        httpExternalEvent.instrumentation = "okhttp-4.4.0";

        if (request != null) {
            httpExternalEvent.path = request.url().encodedPath();
            httpExternalEvent.method = request.method();

            RequestBody requestBody = request.body();
            if (requestBody != null) {
                httpExternalEvent.mediaType = String.valueOf(requestBody.contentType());
                httpExternalEvent.length = Math.toIntExact(requestBody.contentLength());
            }

            httpExternalEvent.queryParameters = request.url().toString();
            httpExternalEvent.headers = request.headers().toString();
            httpExternalEvent.javaMethod = request.method();
        }

        if (response !=  null) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                httpExternalEvent.responseLength = Math.toIntExact(responseBody.contentLength());
            }

            httpExternalEvent.responseHeaders = response.headers().toString();
            httpExternalEvent.status = response.code();
            httpExternalEvent.commit();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "okhttp3.internal.connection.RealCall$AsyncCall")
    public abstract static class AsyncCall_Instrumentation {

        @NewField
        private Token token;

        public AsyncCall_Instrumentation(Callback responseCallback) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }

        @Trace(async = true, leaf = true)
        public void run() {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }

            Weaver.callOriginal();
        }

    }

}
