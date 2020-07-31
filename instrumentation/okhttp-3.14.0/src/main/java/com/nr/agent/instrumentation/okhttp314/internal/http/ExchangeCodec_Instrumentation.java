/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp314.internal.http;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.okhttp314.OkUtils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Weave(type = MatchType.Interface, originalName = "okhttp3.internal.http.ExchangeCodec")
public abstract class ExchangeCodec_Instrumentation {

    public void writeRequestHeaders(Request request) {
        request = OkUtils.doOutboundCAT(request);
        Weaver.callOriginal();
    }

    @Trace
    public Response.Builder readResponseHeaders(boolean expectContinue) {
        return Weaver.callOriginal();
    }

}
