/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFrontEvent;
import com.amazonaws.services.lambda.runtime.events.CodeCommitEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static com.nr.instrumentation.lambda.LambdaConstants.AWS_REQUEST_ID_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_ARN_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_ALB;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_API_GATEWAY;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_CLOUDFRONT;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_CLOUDWATCH_SCHEDULED;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_DYNAMO_STREAMS;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_FIREHOSE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_KINESIS;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_S3;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_SNS;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_TYPE_SQS;
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
        return startTransaction(context, null);
    }

    /**
     * Captures Lambda metadata and event source information.
     *
     * @param context The Lambda execution context
     * @param event The Lambda event object (can be null or any supported event type)
     * @return true if metadata was captured successfully, false otherwise
     */
    public static boolean startTransaction(Context context, Object event) {
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

            if (event != null) {
                extractEventSourceMetadata(event, transaction);
            }

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
     * Extracts event source metadata (ARN and event type) from various AWS Lambda event types.
     * Each event type is handled independently with its own error handling.
     * Adds the aws.lambda.eventSource.arn and aws.lambda.eventSource.eventType attributes when successfully extracted.
     *
     * @param event The Lambda event object (can be any supported event type)
     * @param transaction The current transaction
     */
    public static void extractEventSourceMetadata(Object event, Transaction transaction) {
        if (event == null) {
            return;
        }

        String arn = null;
        String eventType = null;

        // S3Event: Records[0].s3.bucket.arn
        try {
            if (event instanceof S3Event) {
                S3Event s3Event = (S3Event) event;
                if (s3Event.getRecords() != null && !s3Event.getRecords().isEmpty()) {
                    arn = s3Event.getRecords().get(0).getS3().getBucket().getArn();
                }
                eventType = EVENT_TYPE_S3;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from S3Event");
        }

        // SNSEvent: Records[0].EventSubscriptionArn
        try {
            if (event instanceof SNSEvent) {
                SNSEvent snsEvent = (SNSEvent) event;
                if (snsEvent.getRecords() != null && !snsEvent.getRecords().isEmpty()) {
                    arn = snsEvent.getRecords().get(0).getEventSubscriptionArn();
                }
                eventType = EVENT_TYPE_SNS;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from SNSEvent");
        }

        // SQSEvent: Records[0].eventSourceArn
        try {
            if (event instanceof SQSEvent) {
                SQSEvent sqsEvent = (SQSEvent) event;
                if (sqsEvent.getRecords() != null && !sqsEvent.getRecords().isEmpty()) {
                    arn = sqsEvent.getRecords().get(0).getEventSourceArn();
                }
                eventType = EVENT_TYPE_SQS;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from SQSEvent");
        }

        // DynamodbEvent: Records[0].eventSourceARN
        try {
            if (event instanceof DynamodbEvent) {
                DynamodbEvent dynamodbEvent = (DynamodbEvent) event;
                if (dynamodbEvent.getRecords() != null && !dynamodbEvent.getRecords().isEmpty()) {
                    arn = dynamodbEvent.getRecords().get(0).getEventSourceARN();
                }
                eventType = EVENT_TYPE_DYNAMO_STREAMS;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from DynamodbEvent");
        }

        // KinesisEvent: Records[0].eventSourceARN
        try {
            if (event instanceof KinesisEvent) {
                KinesisEvent kinesisEvent = (KinesisEvent) event;
                if (kinesisEvent.getRecords() != null && !kinesisEvent.getRecords().isEmpty()) {
                    arn = kinesisEvent.getRecords().get(0).getEventSourceARN();
                }
                eventType = EVENT_TYPE_KINESIS;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from KinesisEvent");
        }

        // KinesisFirehoseEvent: deliveryStreamArn
        try {
            if (event instanceof KinesisFirehoseEvent) {
                KinesisFirehoseEvent firehoseEvent = (KinesisFirehoseEvent) event;
                arn = firehoseEvent.getDeliveryStreamArn();
                eventType = EVENT_TYPE_FIREHOSE;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from KinesisFirehoseEvent");
        }

        // CodeCommitEvent: Records[0].eventSourceArn
        // Note: CodeCommit is not in the spec, so we extract ARN but omit eventType
        try {
            if (event instanceof CodeCommitEvent) {
                CodeCommitEvent codeCommitEvent = (CodeCommitEvent) event;
                if (codeCommitEvent.getRecords() != null && !codeCommitEvent.getRecords().isEmpty()) {
                    arn = codeCommitEvent.getRecords().get(0).getEventSourceArn();
                }
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from CodeCommitEvent");
        }

        // ScheduledEvent: resources[0]
        try {
            if (event instanceof ScheduledEvent) {
                ScheduledEvent scheduledEvent = (ScheduledEvent) event;
                List<String> resources = scheduledEvent.getResources();
                if (resources != null && !resources.isEmpty()) {
                    arn = resources.get(0);
                }
                eventType = EVENT_TYPE_CLOUDWATCH_SCHEDULED;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from ScheduledEvent");
        }

        // ApplicationLoadBalancerRequestEvent: requestContext.elb.targetGroupArn
        try {
            if (event instanceof ApplicationLoadBalancerRequestEvent) {
                ApplicationLoadBalancerRequestEvent albEvent = (ApplicationLoadBalancerRequestEvent) event;
                if (albEvent.getRequestContext() != null && albEvent.getRequestContext().getElb() != null) {
                    arn = albEvent.getRequestContext().getElb().getTargetGroupArn();
                }
                eventType = EVENT_TYPE_ALB;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from ApplicationLoadBalancerRequestEvent");
        }

        // APIGatewayProxyRequestEvent: No ARN available
        try {
            if (event instanceof APIGatewayProxyRequestEvent) {
                eventType = EVENT_TYPE_API_GATEWAY;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from APIGatewayProxyRequestEvent");
        }

        // APIGatewayV2HTTPEvent: No ARN available (HTTP APIs with payload format v2.0)
        try {
            if (event instanceof APIGatewayV2HTTPEvent) {
                eventType = EVENT_TYPE_API_GATEWAY;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from APIGatewayV2HTTPEvent");
        }

        // CloudFrontEvent: No ARN available
        try {
            if (event instanceof CloudFrontEvent) {
                eventType = EVENT_TYPE_CLOUDFRONT;
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from CloudFrontEvent");
        }

        // Add the ARN attribute if we successfully extracted one
        if (arn != null && !arn.isEmpty()) {
            try {
                transaction.getAgentAttributes().put(EVENT_SOURCE_ARN_ATTRIBUTE, arn);
            } catch (Throwable t) {
                NewRelic.getAgent().getLogger().log(Level.WARNING, t, "Error adding event source ARN attribute");
            }
        }

        // Add the event type attribute if we identified one
        if (eventType != null) {
            try {
                transaction.getAgentAttributes().put(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, eventType);
            } catch (Throwable t) {
                NewRelic.getAgent().getLogger().log(Level.WARNING, t, "Error adding event source event type attribute");
            }
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