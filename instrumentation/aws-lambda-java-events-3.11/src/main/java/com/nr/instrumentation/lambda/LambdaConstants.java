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

    // Event-specific metadata attribute constants
    public static final String EVENT_SOURCE_ACCOUNT = "aws.lambda.eventSource.account";
    public static final String EVENT_SOURCE_ACCOUNT_ID = "aws.lambda.eventSource.accountId";
    public static final String EVENT_SOURCE_API_ID = "aws.lambda.eventSource.apiId";
    public static final String EVENT_SOURCE_BUCKET_NAME = "aws.lambda.eventSource.bucketName";
    public static final String EVENT_SOURCE_EVENT_NAME = "aws.lambda.eventSource.eventName";
    public static final String EVENT_SOURCE_EVENT_TIME = "aws.lambda.eventSource.eventTime";
    public static final String EVENT_SOURCE_ID = "aws.lambda.eventSource.id";
    public static final String EVENT_SOURCE_LENGTH = "aws.lambda.eventSource.length";
    public static final String EVENT_SOURCE_MESSAGE_ID = "aws.lambda.eventSource.messageId";
    public static final String EVENT_SOURCE_OBJECT_KEY = "aws.lambda.eventSource.objectKey";
    public static final String EVENT_SOURCE_OBJECT_SEQUENCER = "aws.lambda.eventSource.objectSequencer";
    public static final String EVENT_SOURCE_OBJECT_SIZE = "aws.lambda.eventSource.objectSize";
    public static final String EVENT_SOURCE_REGION = "aws.lambda.eventSource.region";
    public static final String EVENT_SOURCE_RESOURCE = "aws.lambda.eventSource.resource";
    public static final String EVENT_SOURCE_RESOURCE_ID = "aws.lambda.eventSource.resourceId";
    public static final String EVENT_SOURCE_RESOURCE_PATH = "aws.lambda.eventSource.resourcePath";
    public static final String EVENT_SOURCE_STAGE = "aws.lambda.eventSource.stage";
    public static final String EVENT_SOURCE_TIME = "aws.lambda.eventSource.time";
    public static final String EVENT_SOURCE_TIMESTAMP = "aws.lambda.eventSource.timestamp";
    public static final String EVENT_SOURCE_TOPIC_ARN = "aws.lambda.eventSource.topicArn";
    public static final String EVENT_SOURCE_TYPE = "aws.lambda.eventSource.type";
    public static final String EVENT_SOURCE_X_AMZ_ID_2 = "aws.lambda.eventSource.xAmzId2";

    private LambdaConstants() {
        // Prevent instantiation
    }
}