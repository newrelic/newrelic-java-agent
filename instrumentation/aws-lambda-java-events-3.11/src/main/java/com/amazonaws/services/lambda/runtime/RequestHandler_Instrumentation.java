/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda.runtime;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.lambda.LambdaInstrumentationHelper;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.lambda.runtime.RequestHandler")
public abstract class RequestHandler_Instrumentation<I, O> {

    /**
     * Intercepts the Lambda handler method to instrument it with New Relic monitoring.
     *
     * @param input The Lambda function input (event data)
     * @param context The Lambda execution context containing function metadata
     * @return The handler's return value
     */
    @Trace(dispatcher = true)
    public O handleRequest(I input, Context context) {
        try {
            LambdaInstrumentationHelper.startTransaction(context);
        } catch (Throwable t) {

        }

        O result = Weaver.callOriginal();

        try {
            LambdaInstrumentationHelper.finishTransaction();
        } catch (Throwable t) {

        }

        return result;
    }
}
