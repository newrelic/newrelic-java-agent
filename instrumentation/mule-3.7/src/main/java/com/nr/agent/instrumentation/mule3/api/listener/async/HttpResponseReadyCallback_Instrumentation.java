/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api.listener.async;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

@Weave(type = MatchType.Interface, originalName = "org.mule.module.http.internal.listener.async.HttpResponseReadyCallback")
public abstract class HttpResponseReadyCallback_Instrumentation {

    @Trace
    public void responseReady(HttpResponse response, ResponseStatusCallback responseStatusCallback) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Mule", "HttpResponse", "responseReady");
        Weaver.callOriginal();
    }

}
