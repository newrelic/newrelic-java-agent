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
import okhttp3.Response;

@Weave(type = MatchType.ExactClass, originalName = "okhttp3.internal.http.HttpEngine")
public abstract class HttpEngine_Instrumentation {

    @Trace
    private Response readNetworkResponse() {
        return Weaver.callOriginal();
    }

    @Trace
    public void sendRequest() {
        Weaver.callOriginal();
    }

    @Trace
    public void readResponse() {
        Weaver.callOriginal();
    }

}
