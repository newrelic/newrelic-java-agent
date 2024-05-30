/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.marshalling;

import org.apache.pekko.http.scaladsl.model.HttpResponse;
import com.agent.instrumentation.org.apache.pekko.http.ResponseWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weaver;
import scala.runtime.AbstractFunction1;

public class PekkoHttpMarshallerMapper extends AbstractFunction1<HttpResponse, HttpResponse> {

    private final Token token;

    public PekkoHttpMarshallerMapper(Token token) {
        this.token = token;
    }

    @Override
    @Trace(async = true)
    public HttpResponse apply(HttpResponse httpResponse) {
        try {
            if (token != null) {
                token.linkAndExpire();
            }
            ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
            Transaction transaction = NewRelic.getAgent().getTransaction();
            transaction.setWebResponse(responseWrapper);
            transaction.addOutboundResponseHeaders();
            transaction.markResponseSent();

            return responseWrapper.response();
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
            return httpResponse;
        }
    }

}
