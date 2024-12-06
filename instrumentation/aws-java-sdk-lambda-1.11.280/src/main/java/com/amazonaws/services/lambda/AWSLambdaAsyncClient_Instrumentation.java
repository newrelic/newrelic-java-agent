/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda;

import com.agent.instrumentation.awsjavasdk1.services.lambda.LambdaUtil;
import com.agent.instrumentation.awsjavasdk1.services.lambda.TokenLinkingAsyncHandler;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Future;

@Weave(type = MatchType.ExactClass, originalName = "com.amazonaws.services.lambda.AWSLambdaAsyncClient")
public abstract class AWSLambdaAsyncClient_Instrumentation {

    @Trace
    public Future<InvokeResult> invokeAsync(final InvokeRequest request, AsyncHandler<InvokeRequest, InvokeResult> asyncHandler) {
        LambdaUtil.setTokenForRequest(request);
        if (asyncHandler != null) {
            asyncHandler = new TokenLinkingAsyncHandler(asyncHandler);
        }
        return Weaver.callOriginal();
    }
}
