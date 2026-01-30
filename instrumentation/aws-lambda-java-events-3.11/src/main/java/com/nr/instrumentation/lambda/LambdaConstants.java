/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda;

// Constants for AWS Lambda instrumentation.
public class LambdaConstants {
    // Attribute key constants
    public static final String LAMBDA_ARN_ATTRIBUTE = "aws.lambda.arn";
    public static final String LAMBDA_COLD_START_ATTRIBUTE = "aws.lambda.coldStart";
    public static final String AWS_REQUEST_ID_ATTRIBUTE = "aws.requestId";
    public static final String EVENT_SOURCE_ARN_ATTRIBUTE = "aws.lambda.eventSource.arn";
    public static final String EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE = "aws.lambda.eventSource.eventType";

    // Event type value constants
    public static final String EVENT_TYPE_S3 = "s3";
    public static final String EVENT_TYPE_SNS = "sns";
    public static final String EVENT_TYPE_SQS = "sqs";
    public static final String EVENT_TYPE_DYNAMO_STREAMS = "dynamo_streams";
    public static final String EVENT_TYPE_KINESIS = "kinesis";
    public static final String EVENT_TYPE_FIREHOSE = "firehose";
    public static final String EVENT_TYPE_CLOUDWATCH_SCHEDULED = "cloudWatch_scheduled";
    public static final String EVENT_TYPE_ALB = "alb";
    public static final String EVENT_TYPE_API_GATEWAY = "apiGateway";
    public static final String EVENT_TYPE_CLOUDFRONT = "cloudFront";

    private LambdaConstants() {
        // Prevent instantiation
    }
}