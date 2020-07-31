/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp30.internal.http;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RetryableSink;

@Weave(type = MatchType.Interface, originalName = "okhttp3.internal.http.HttpStream")
public abstract class HttpStream_Instrumentation {

    @Trace
    public void writeRequestHeaders(Request request) {
        Weaver.callOriginal();
    }

    @Trace
    public Response.Builder readResponseHeaders() {
        return Weaver.callOriginal();
    }

    @Trace
    public void writeRequestBody(RetryableSink requestBody) {
        Weaver.callOriginal();
    }

    @Trace
    public ResponseBody openResponseBody(Response response) {
        return Weaver.callOriginal();
    }

}
