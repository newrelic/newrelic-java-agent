/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.lambda;

import com.agent.instrumentation.awsjavasdk2.services.lambda.FunctionRawData;
import com.agent.instrumentation.awsjavasdk2.services.lambda.LambdaUtil;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.lambda.DefaultLambdaAsyncClient")
final class DefaultLambdaAsyncClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    public CompletableFuture<InvokeResponse> invoke(InvokeRequest invokeRequest) {
        FunctionRawData functionRawData = new FunctionRawData(invokeRequest.functionName(), invokeRequest.qualifier(), clientConfiguration);
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        String functionName = LambdaUtil.getSimpleFunctionName(functionRawData);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Lambda", "invoke/" + functionName);

        try {
            segment.reportAsExternal(cloudParameters);
            CompletableFuture<InvokeResponse> invokeResponseCompletableFuture = Weaver.callOriginal();

            return invokeResponseCompletableFuture.whenComplete(new SegmentFinisher(segment));
        } catch (Throwable t) {

            segment.end();
            throw t;
        }
    }

    private static class SegmentFinisher implements BiConsumer<InvokeResponse, Throwable> {
        private final Segment segment;

        public SegmentFinisher(Segment segment) {
            this.segment = segment;
        }

        @Override
        public void accept(InvokeResponse invokeResponse, Throwable throwable) {
            segment.end();
        }
    }
}
