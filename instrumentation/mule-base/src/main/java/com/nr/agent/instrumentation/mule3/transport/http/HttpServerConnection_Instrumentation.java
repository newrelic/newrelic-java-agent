/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.transport.http;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleUtils;
import org.mule.transport.http.HttpResponse;

import java.util.Map;

/**
 * A connection to the SimpleHttpServer.
 */
@Weave(type = MatchType.ExactClass, originalName = "org.mule.transport.http.HttpServerConnection")
public abstract class HttpServerConnection_Instrumentation {

    @Trace
    public void writeResponse(final HttpResponse response, Map<String, String> headers) {
        MuleUtils.reportToAgent(response);
        Weaver.callOriginal();
    }

}
