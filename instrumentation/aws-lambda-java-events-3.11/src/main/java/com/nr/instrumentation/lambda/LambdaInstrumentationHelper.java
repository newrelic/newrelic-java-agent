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

import static com.nr.instrumentation.lambda.LambdaConstants.AWS_REQUEST_ID_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.LAMBDA_ARN_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.LAMBDA_COLD_START_ATTRIBUTE;

/**
 * Helper class for Lambda instrumentation.
 * Uses AgentBridge.serverlessApi to communicate metadata to the core agent
 * without creating circular dependencies.
 */
public class LambdaInstrumentationHelper {

    // Track whether this is the first invocation (cold start)
    private static final AtomicBoolean COLD_START = new AtomicBoolean(true);

    /**
     * Captures Lambda metadata and stores it via AgentBridge for the serverless payload.
     *
     * @param context The Lambda execution context
     * @return true if metadata was captured successfully, false otherwise
     */
    public static boolean startTransaction(Context context) {
        if (context == null) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "Lambda Context is null, cannot capture metadata");
            return false;
        }

        try {
            Transaction transaction = AgentBridge.getAgent().getTransaction(false);
            if (transaction == null) {
                NewRelic.getAgent().getLogger().log(Level.FINE, "No transaction available");
                return false;
            }

            captureLambdaMetadata(context, transaction);

            handleColdStart(transaction);

            return true;
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, t, "Error capturing Lambda metadata");
            return false;
        }
    }

    /**
     * Captures Lambda-specific metadata from the Context via AgentBridge.
     * Stores ARN and function version for inclusion in serverless payload envelope.
     * Also adds agent attributes for arn and requestId.
     *
     * Each attribute is extracted independently with its own error handling to ensure
     * that a failure extracting one attribute doesn't impact the extraction of another.
     *
     * @param context The Lambda execution context
     * @param transaction The current transaction
     */
    private static void captureLambdaMetadata(Context context, Transaction transaction) {
        try {
            String arn = context.getInvokedFunctionArn();
            if (arn != null && !arn.isEmpty()) {
                AgentBridge.serverlessApi.setArn(arn);
                transaction.getAgentAttributes().put(LAMBDA_ARN_ATTRIBUTE, arn);
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error capturing Lambda ARN");
        }

        try {
            String functionVersion = context.getFunctionVersion();
            if (functionVersion != null && !functionVersion.isEmpty()) {
                AgentBridge.serverlessApi.setFunctionVersion(functionVersion);
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error capturing Lambda function version");
        }

        try {
            String requestId = context.getAwsRequestId();
            if (requestId != null && !requestId.isEmpty()) {
                transaction.getAgentAttributes().put(AWS_REQUEST_ID_ATTRIBUTE, requestId);
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error capturing Lambda request ID");
        }
    }

    /**
     * Tracks whether this is a cold start (first invocation).
     * Adds the aws.lambda.coldStart agent attribute when it's a cold start.
     * According to the Lambda spec, the attribute should only be added when true (omitted when false).
     *
     * @param transaction The current transaction
     */
    private static void handleColdStart(Transaction transaction) {
        try {
            // Check if this is a cold start (first invocation)
            boolean isColdStart = COLD_START.compareAndSet(true, false);

            if (isColdStart) {
                transaction.getAgentAttributes().put(LAMBDA_COLD_START_ATTRIBUTE, true);
                NewRelic.getAgent().getLogger().log(Level.FINE, "Cold start detected, added aws.lambda.coldStart attribute");
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

    /**
     * Resets the cold start state for testing purposes only.
     */
    public static void resetColdStartForTesting() {
        COLD_START.set(true);
    }
}