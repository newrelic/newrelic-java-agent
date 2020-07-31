/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api.listener.async;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;

@Weave(type = MatchType.Interface, originalName = "org.mule.module.http.internal.listener.async.RequestHandler")
public abstract class RequestHandler_Instrumentation {

    @Trace(excludeFromTransactionTrace = true)
    public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
        Weaver.callOriginal();
    }

}
