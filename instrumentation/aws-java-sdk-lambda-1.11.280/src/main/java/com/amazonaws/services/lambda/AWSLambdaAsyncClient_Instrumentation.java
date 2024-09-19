/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda;

import com.agent.instrumentation.awsjavasdk1.services.lambda.FunctionRawData;
import com.agent.instrumentation.awsjavasdk1.services.lambda.LambdaUtil;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Future;

@Weave(type = MatchType.ExactClass, originalName = "com.amazonaws.services.lambda.AWSLambdaAsyncClient")
public abstract class AWSLambdaAsyncClient_Instrumentation {

    protected abstract String getSigningRegion();

    public Future<InvokeResult> invokeAsync(final InvokeRequest request, AsyncHandler<InvokeRequest, InvokeResult> asyncHandler) {
        FunctionRawData functionRawData = new FunctionRawData(request.getFunctionName(), request.getQualifier(), getSigningRegion(), this);
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        String functionName = LambdaUtil.getSimpleFunctionName(functionRawData);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Lambda", "invoke/" + functionName);

        try {
            segment.reportAsExternal(cloudParameters);
            asyncHandler = new SegmentEndingAsyncHandler(asyncHandler, segment);
            return Weaver.callOriginal();
        } catch (Throwable t) {
            segment.end();
            throw t;
        }
    }

    private static class SegmentEndingAsyncHandler implements AsyncHandler<InvokeRequest, InvokeResult> {
        private final AsyncHandler<InvokeRequest, InvokeResult> originalHandler;
        private final Segment segment;

        public SegmentEndingAsyncHandler(
                AsyncHandler<InvokeRequest, InvokeResult> asyncHandler, Segment segment) {
            this.segment = segment;
            this.originalHandler = asyncHandler;
        }

        @Override
        public void onError(Exception exception) {
            segment.end();
            if (originalHandler != null) {
                originalHandler.onError(exception);
            }
        }

        @Override
        public void onSuccess(InvokeRequest request, InvokeResult invokeResult) {
            segment.end();
            if (originalHandler != null) {
                originalHandler.onSuccess(request, invokeResult);
            }
        }
    }
}
