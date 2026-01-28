/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda;

// Constants for AWS Lambda instrumentation.
public class LambdaConstants {
    public static final String LAMBDA_ARN_ATTRIBUTE = "aws.lambda.arn";
    public static final String LAMBDA_COLD_START_ATTRIBUTE = "aws.lambda.coldStart";
    public static final String AWS_REQUEST_ID_ATTRIBUTE = "aws.requestId";

    private LambdaConstants() {
        // Prevent instantiation
    }
}