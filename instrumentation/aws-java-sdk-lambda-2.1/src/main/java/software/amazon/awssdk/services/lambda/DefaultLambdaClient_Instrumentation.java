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
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.lambda.DefaultLambdaClient")
final class DefaultLambdaClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace(leaf = true)
    public InvokeResponse invoke(InvokeRequest invokeRequest) {
        FunctionRawData functionRawData = new FunctionRawData(invokeRequest.functionName(), invokeRequest.qualifier(), clientConfiguration);
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.reportAsExternal(cloudParameters);
        tracedMethod.setMetricName("Lambda", "invoke", LambdaUtil.getSimpleFunctionName(functionRawData));
        return Weaver.callOriginal();
    }
}

