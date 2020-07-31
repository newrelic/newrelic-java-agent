/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.internal.domain.response;

import org.apache.commons.collections.MultiMap;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.domain.response.ResponseStatus;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleHttpConnectorResponse;

@Weave(type = MatchType.ExactClass, originalName = "org.mule.module.http.internal.domain.response.HttpResponseBuilder")
public class HttpResponseBuilder_Instrumentation {

    private MultiMap headers = Weaver.callOriginal();
    private ResponseStatus responseStatus = Weaver.callOriginal();

    @Trace(excludeFromTransactionTrace = true)
    public HttpResponse build() {
        MuleHttpConnectorResponse response = new MuleHttpConnectorResponse(headers, responseStatus);
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(response);

        final Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            txn.setWebResponse(response);
            long contentLength;
            try {
                contentLength = getContentLength(response);
            } catch (Exception e) {
                contentLength = -1L;
            }
            txn.getCrossProcessState().processOutboundResponseHeaders(response, contentLength);
        }

        return Weaver.callOriginal();
    }

    private static long getContentLength(MuleHttpConnectorResponse muleResponse) {
        String contentLength = muleResponse.getHeader("Content-Length");
        if (contentLength == null || contentLength.isEmpty()) {
            return -1L;
        } else {
            return Long.parseLong(contentLength);
        }
    }

}
