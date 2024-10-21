/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda;

import com.agent.instrumentation.awsjavasdk1.services.lambda.FunctionRawData;
import com.agent.instrumentation.awsjavasdk1.services.lambda.LambdaUtil;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.CatchAndLog;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "com.amazonaws.services.lambda.AWSLambdaClient")
public abstract class AWSLambdaClient_Instrumentation {

    abstract protected String getSigningRegion();

    @Trace(leaf = true)
    public InvokeResult invoke(InvokeRequest invokeRequest) {
        FunctionRawData functionRawData = new FunctionRawData(invokeRequest.getFunctionName(), invokeRequest.getQualifier(), getSigningRegion());
        CloudParameters cloudParameters = LambdaUtil.getCloudParameters(functionRawData);
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.reportAsExternal(cloudParameters);
        tracedMethod.setMetricName("Lambda", "invoke", LambdaUtil.getSimpleFunctionName(functionRawData));
        return Weaver.callOriginal();
    }

}

