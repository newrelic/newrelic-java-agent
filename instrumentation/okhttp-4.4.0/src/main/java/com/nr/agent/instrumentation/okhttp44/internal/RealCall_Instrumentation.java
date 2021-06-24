/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp44.internal;

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
import okhttp3.Response;

@Weave(type = MatchType.ExactClass, originalName = "okhttp3.internal.connection.RealCall")
public abstract class RealCall_Instrumentation {

    public Request getOriginalRequest() {
        return Weaver.callOriginal();
    }

    @Trace(external = true, leaf = true)
    public Response execute() {
        return Weaver.callOriginal();
    }

    @Trace(external = true)
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
            throw e;
        }

        OkUtils.processResponse(getOriginalRequest().url().uri(), response);

        return response;
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
