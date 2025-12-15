/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class LambdaInstrumentationHelper {

    // Track whether this is the first invocation (cold start)
    private static final AtomicBoolean COLD_START = new AtomicBoolean(true);

    /**
     * Starts a New Relic transaction for a Lambda invocation and captures metadata.
     *
     * @param context The Lambda execution context
     * @return true if transaction was started successfully, false otherwise
     */
    public static boolean startTransaction(Context context) {
        if (context == null) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "Lambda Context is null, cannot start transaction");
            return false;
        }

        try {
            Transaction transaction = AgentBridge.getAgent().getTransaction(false);
            if (transaction == null) {
                NewRelic.getAgent().getLogger().log(Level.FINE, "No transaction available");
                return false;
            }

            String functionName = context.getFunctionName();
            if (functionName != null && !functionName.isEmpty()) {
                NewRelic.setTransactionName("Function", functionName);
            }

            captureLambdaMetadata(context);

            handleColdStart();

            return true;
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, t, "Error starting Lambda transaction");
            return false;
        }
    }

    /**
     * Captures Lambda-specific metadata from the Context and adds it to the transaction.
     * Stores ARN and function version in LambdaMetadataProvider for serverless payload.
     *
     * @param context The Lambda execution context
     */
    private static void captureLambdaMetadata(Context context) {
        try {
            String arn = context.getInvokedFunctionArn();
            if (arn != null && !arn.isEmpty()) {
                NewRelic.addCustomParameter("aws.lambda.arn", arn);
                LambdaMetadataProvider.setArn(arn);
            }

            String functionVersion = context.getFunctionVersion();
            if (functionVersion != null && !functionVersion.isEmpty()) {
                NewRelic.addCustomParameter("aws.lambda.function_version", functionVersion);
                LambdaMetadataProvider.setFunctionVersion(functionVersion);
            }

        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, t, "Error capturing Lambda metadata");
        }
    }

    /**
     * Tracks whether this is a cold start (first invocation).
     * Sets a custom parameter only for the first invocation.
     */
    private static void handleColdStart() {
        try {
            // Only set to true for the first invocation
            if (COLD_START.compareAndSet(true, false)) {
                NewRelic.addCustomParameter("aws.lambda.coldStart", true);
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, t, "Error handling cold start");
        }
    }

    /**
     * Finishes the Lambda transaction.
     * The transaction will naturally end when the handler method returns.
     * This method is here for explicit cleanup if needed in the future.
     */
    public static void finishTransaction() {
        // This method is a placeholder for any future cleanup logic
    }
}
