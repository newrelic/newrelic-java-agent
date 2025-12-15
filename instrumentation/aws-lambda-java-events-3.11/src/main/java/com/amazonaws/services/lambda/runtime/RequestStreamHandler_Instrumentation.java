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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.lambda.runtime.RequestStreamHandler")
public abstract class RequestStreamHandler_Instrumentation {

    /**
     * Intercepts the Lambda stream handler method to instrument it with New Relic monitoring.
     *
     * @param input The Lambda function input stream
     * @param output The Lambda function output stream
     * @param context The Lambda execution context containing function metadata
     * @throws IOException If an I/O error occurs
     */
    @Trace(dispatcher = true)
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        try {
            // Start transaction and capture Lambda metadata
            LambdaInstrumentationHelper.startTransaction(context);
        } catch (Throwable t) {

        }

        // Execute the original handler method
        Weaver.callOriginal();

        try {
            // Finish transaction - this triggers serverless harvest
            LambdaInstrumentationHelper.finishTransaction();
        } catch (Throwable t) {

        }
    }
}
