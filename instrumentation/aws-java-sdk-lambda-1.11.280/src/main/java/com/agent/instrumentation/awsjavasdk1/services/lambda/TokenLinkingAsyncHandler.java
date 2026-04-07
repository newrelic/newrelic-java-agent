/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class TokenLinkingAsyncHandler implements AsyncHandler<InvokeRequest, InvokeResult> {

    private final AsyncHandler<InvokeRequest, InvokeResult> delegate;

    private Token token;

    public TokenLinkingAsyncHandler(AsyncHandler<InvokeRequest, InvokeResult> delegate) {
        this.delegate = delegate;
        token = NewRelic.getAgent().getTransaction().getToken();
    }

    @Override
    @Trace(async = true)
    public void onError(Exception e) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        NewRelic.getAgent().getTracedMethod().setMetricName("Java", delegate.getClass().getName(), "onError");
        delegate.onError(e);
    }

    @Override
    @Trace(async = true)
    public void onSuccess(InvokeRequest request, InvokeResult invokeResult) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        NewRelic.getAgent().getTracedMethod().setMetricName("Java", delegate.getClass().getName(), "onSuccess");
        delegate.onSuccess(request, invokeResult);
    }
}
