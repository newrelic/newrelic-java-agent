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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import static com.nr.instrumentation.lambda.LambdaConstants.AWS_REQUEST_ID_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_ACCOUNT;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_ACCOUNT_ID;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_API_ID;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_ARN_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_BUCKET_NAME;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_EVENT_NAME;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_EVENT_TIME;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_ID;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_LENGTH;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_MESSAGE_ID;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_OBJECT_KEY;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_OBJECT_SEQUENCER;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_OBJECT_SIZE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_REGION;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_RESOURCE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_RESOURCE_ID;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_RESOURCE_PATH;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_STAGE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_TIME;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_TIMESTAMP;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_TOPIC_ARN;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_TYPE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_X_AMZ_ID_2;
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

    // Map of event types to their extraction handlers
    private static final Map<Class<?>, BiConsumer<Object, Transaction>> EVENT_EXTRACTORS = new HashMap<>();

    static {
        EVENT_EXTRACTORS.put(S3Event.class, (event, txn) -> extractS3Metadata((S3Event) event, txn));
        EVENT_EXTRACTORS.put(SNSEvent.class, (event, txn) -> extractSNSMetadata((SNSEvent) event, txn));
        EVENT_EXTRACTORS.put(SQSEvent.class, (event, txn) -> extractSQSMetadata((SQSEvent) event, txn));
        EVENT_EXTRACTORS.put(DynamodbEvent.class, (event, txn) -> extractDynamodbMetadata((DynamodbEvent) event, txn));
        EVENT_EXTRACTORS.put(KinesisEvent.class, (event, txn) -> extractKinesisMetadata((KinesisEvent) event, txn));
        EVENT_EXTRACTORS.put(KinesisFirehoseEvent.class, (event, txn) -> extractKinesisFirehoseMetadata((KinesisFirehoseEvent) event, txn));
        EVENT_EXTRACTORS.put(CodeCommitEvent.class, (event, txn) -> extractCodeCommitMetadata((CodeCommitEvent) event, txn));
        EVENT_EXTRACTORS.put(ScheduledEvent.class, (event, txn) -> extractScheduledEventMetadata((ScheduledEvent) event, txn));
        EVENT_EXTRACTORS.put(ApplicationLoadBalancerRequestEvent.class, (event, txn) -> extractALBMetadata((ApplicationLoadBalancerRequestEvent) event, txn));
        EVENT_EXTRACTORS.put(APIGatewayProxyRequestEvent.class, (event, txn) -> extractAPIGatewayProxyMetadata((APIGatewayProxyRequestEvent) event, txn));
        EVENT_EXTRACTORS.put(APIGatewayV2HTTPEvent.class, (event, txn) -> extractAPIGatewayV2HTTPMetadata((APIGatewayV2HTTPEvent) event, txn));
        EVENT_EXTRACTORS.put(CloudFrontEvent.class, (event, txn) -> extractCloudFrontMetadata((CloudFrontEvent) event, txn));
    }

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
     * Uses a map-based dispatcher to delegate to specific extraction methods for each event type.
     *
     * @param event The Lambda event object (can be any supported event type)
     * @param transaction The current transaction
     */
    public static void extractEventSourceMetadata(Object event, Transaction transaction) {
        if (event == null) {
            return;
        }

        BiConsumer<Object, Transaction> extractor = EVENT_EXTRACTORS.get(event.getClass());
        if (extractor != null) {
            extractor.accept(event, transaction);
        }
    }

    /**
     * Helper method to safely add a string attribute to the transaction.
     *
     * @param transaction The current transaction
     * @param key The attribute key
     * @param value The attribute value
     */
    private static void addAttribute(Transaction transaction, String key, String value) {
        if (value != null && !value.isEmpty()) {
            try {
                transaction.getAgentAttributes().put(key, value);
            } catch (Throwable t) {
                NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error adding attribute: " + key);
            }
        }
    }

    /**
     * Helper method to safely add an integer attribute to the transaction.
     *
     * @param transaction The current transaction
     * @param key The attribute key
     * @param value The attribute value
     */
    private static void addAttribute(Transaction transaction, String key, Integer value) {
        if (value != null) {
            try {
                transaction.getAgentAttributes().put(key, value);
            } catch (Throwable t) {
                NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error adding attribute: " + key);
            }
        }
    }

    /**
     * Helper method to safely add a long attribute to the transaction.
     *
     * @param transaction The current transaction
     * @param key The attribute key
     * @param value The attribute value
     */
    private static void addAttribute(Transaction transaction, String key, Long value) {
        if (value != null) {
            try {
                transaction.getAgentAttributes().put(key, value);
            } catch (Throwable t) {
                NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error adding attribute: " + key);
            }
        }
    }

    /**
     * Extracts metadata from S3Event.
     * Adds event source ARN, event type, and S3-specific attributes.
     */
    private static void extractS3Metadata(S3Event event, Transaction transaction) {
        try {
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                S3Event.S3EventNotificationRecord firstRecord = event.getRecords().get(0);

                // ARN and event type
                if (firstRecord.getS3() != null && firstRecord.getS3().getBucket() != null) {
                    addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, firstRecord.getS3().getBucket().getArn());
                }
                addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_S3);

                // S3-specific attributes
                addAttribute(transaction, EVENT_SOURCE_LENGTH, event.getRecords().size());
                addAttribute(transaction, EVENT_SOURCE_REGION, firstRecord.getAwsRegion());
                addAttribute(transaction, EVENT_SOURCE_EVENT_NAME, firstRecord.getEventName());
                addAttribute(transaction, EVENT_SOURCE_EVENT_TIME, firstRecord.getEventTime() != null ? firstRecord.getEventTime().toString() : null);

                if (firstRecord.getResponseElements() != null) {
                    addAttribute(transaction, EVENT_SOURCE_X_AMZ_ID_2, firstRecord.getResponseElements().getxAmzId2());
                }

                if (firstRecord.getS3() != null) {
                    if (firstRecord.getS3().getBucket() != null) {
                        addAttribute(transaction, EVENT_SOURCE_BUCKET_NAME, firstRecord.getS3().getBucket().getName());
                    }
                    if (firstRecord.getS3().getObject() != null) {
                        addAttribute(transaction, EVENT_SOURCE_OBJECT_KEY, firstRecord.getS3().getObject().getKey());
                        addAttribute(transaction, EVENT_SOURCE_OBJECT_SEQUENCER, firstRecord.getS3().getObject().getSequencer());
                        addAttribute(transaction, EVENT_SOURCE_OBJECT_SIZE, firstRecord.getS3().getObject().getSizeAsLong());
                    }
                }
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from S3Event");
        }
    }

    /**
     * Extracts metadata from SNSEvent.
     * Adds event source ARN, event type, and SNS-specific attributes.
     */
    private static void extractSNSMetadata(SNSEvent event, Transaction transaction) {
        try {
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                SNSEvent.SNSRecord firstRecord = event.getRecords().get(0);

                // ARN and event type
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, firstRecord.getEventSubscriptionArn());
                addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_SNS);

                // SNS-specific attributes
                addAttribute(transaction, EVENT_SOURCE_LENGTH, event.getRecords().size());
                if (firstRecord.getSNS() != null) {
                    addAttribute(transaction, EVENT_SOURCE_MESSAGE_ID, firstRecord.getSNS().getMessageId());
                    addAttribute(transaction, EVENT_SOURCE_TIMESTAMP, firstRecord.getSNS().getTimestamp() != null ? firstRecord.getSNS().getTimestamp().toString() : null);
                    addAttribute(transaction, EVENT_SOURCE_TOPIC_ARN, firstRecord.getSNS().getTopicArn());
                    addAttribute(transaction, EVENT_SOURCE_TYPE, firstRecord.getSNS().getType());
                }
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from SNSEvent");
        }
    }

    /**
     * Extracts metadata from SQSEvent.
     * Adds event source ARN, event type, and SQS-specific attributes.
     */
    private static void extractSQSMetadata(SQSEvent event, Transaction transaction) {
        try {
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                // ARN and event type
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, event.getRecords().get(0).getEventSourceArn());
                addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_SQS);

                // SQS-specific attributes
                addAttribute(transaction, EVENT_SOURCE_LENGTH, event.getRecords().size());
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from SQSEvent");
        }
    }

    /**
     * Extracts metadata from DynamodbEvent.
     * Adds event source ARN and event type.
     */
    private static void extractDynamodbMetadata(DynamodbEvent event, Transaction transaction) {
        try {
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                // ARN and event type
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, event.getRecords().get(0).getEventSourceARN());
                addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_DYNAMO_STREAMS);
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from DynamodbEvent");
        }
    }

    /**
     * Extracts metadata from KinesisEvent.
     * Adds event source ARN, event type, and Kinesis-specific attributes.
     */
    private static void extractKinesisMetadata(KinesisEvent event, Transaction transaction) {
        try {
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                KinesisEvent.KinesisEventRecord firstRecord = event.getRecords().get(0);

                // ARN and event type
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, firstRecord.getEventSourceARN());
                addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_KINESIS);

                // Kinesis-specific attributes
                addAttribute(transaction, EVENT_SOURCE_LENGTH, event.getRecords().size());
                addAttribute(transaction, EVENT_SOURCE_REGION, firstRecord.getAwsRegion());
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from KinesisEvent");
        }
    }

    /**
     * Extracts metadata from KinesisFirehoseEvent.
     * Adds event source ARN, event type, and Kinesis Firehose-specific attributes.
     */
    private static void extractKinesisFirehoseMetadata(KinesisFirehoseEvent event, Transaction transaction) {
        try {
            // ARN and event type
            addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, event.getDeliveryStreamArn());
            addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_FIREHOSE);

            // Kinesis Firehose-specific attributes
            if (event.getRecords() != null) {
                addAttribute(transaction, EVENT_SOURCE_LENGTH, event.getRecords().size());
            }
            addAttribute(transaction, EVENT_SOURCE_REGION, event.getRegion());
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from KinesisFirehoseEvent");
        }
    }

    /**
     * Extracts metadata from CodeCommitEvent.
     * Note: CodeCommit is not in the spec, so we only extract ARN without event type.
     */
    private static void extractCodeCommitMetadata(CodeCommitEvent event, Transaction transaction) {
        try {
            if (event.getRecords() != null && !event.getRecords().isEmpty()) {
                // ARN only (no event type in spec)
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, event.getRecords().get(0).getEventSourceArn());
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from CodeCommitEvent");
        }
    }

    /**
     * Extracts metadata from ScheduledEvent (CloudWatch Scheduled).
     * Adds event source ARN, event type, and CloudWatch Scheduled-specific attributes.
     */
    private static void extractScheduledEventMetadata(ScheduledEvent event, Transaction transaction) {
        try {
            // ARN and event type
            List<String> resources = event.getResources();
            if (resources != null && !resources.isEmpty()) {
                String arn = resources.get(0);
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, arn);
                addAttribute(transaction, EVENT_SOURCE_RESOURCE, arn);
            }
            addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_CLOUDWATCH_SCHEDULED);

            // CloudWatch Scheduled-specific attributes
            addAttribute(transaction, EVENT_SOURCE_ACCOUNT, event.getAccount());
            addAttribute(transaction, EVENT_SOURCE_ID, event.getId());
            addAttribute(transaction, EVENT_SOURCE_REGION, event.getRegion());
            addAttribute(transaction, EVENT_SOURCE_TIME, event.getTime() != null ? event.getTime().toString() : null);
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from ScheduledEvent");
        }
    }

    /**
     * Extracts metadata from ApplicationLoadBalancerRequestEvent.
     * Adds event source ARN and event type.
     */
    private static void extractALBMetadata(ApplicationLoadBalancerRequestEvent event, Transaction transaction) {
        try {
            // ARN and event type
            if (event.getRequestContext() != null && event.getRequestContext().getElb() != null) {
                addAttribute(transaction, EVENT_SOURCE_ARN_ATTRIBUTE, event.getRequestContext().getElb().getTargetGroupArn());
            }
            addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_ALB);
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from ApplicationLoadBalancerRequestEvent");
        }
    }

    /**
     * Extracts metadata from APIGatewayProxyRequestEvent.
     * Adds event type and API Gateway-specific attributes.
     */
    private static void extractAPIGatewayProxyMetadata(APIGatewayProxyRequestEvent event, Transaction transaction) {
        try {
            // Event type
            addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_API_GATEWAY);

            // API Gateway-specific attributes
            if (event.getRequestContext() != null) {
                addAttribute(transaction, EVENT_SOURCE_ACCOUNT_ID, event.getRequestContext().getAccountId());
                addAttribute(transaction, EVENT_SOURCE_API_ID, event.getRequestContext().getApiId());
                addAttribute(transaction, EVENT_SOURCE_RESOURCE_ID, event.getRequestContext().getResourceId());
                addAttribute(transaction, EVENT_SOURCE_RESOURCE_PATH, event.getRequestContext().getResourcePath());
                addAttribute(transaction, EVENT_SOURCE_STAGE, event.getRequestContext().getStage());
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from APIGatewayProxyRequestEvent");
        }
    }

    /**
     * Extracts metadata from APIGatewayV2HTTPEvent.
     * Adds event type and API Gateway V2-specific attributes.
     * Note: V2 uses routeKey instead of resourceId/resourcePath from V1.
     */
    private static void extractAPIGatewayV2HTTPMetadata(APIGatewayV2HTTPEvent event, Transaction transaction) {
        try {
            // Event type
            addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_API_GATEWAY);

            // API Gateway V2-specific attributes (no resourceId/resourcePath in V2)
            if (event.getRequestContext() != null) {
                addAttribute(transaction, EVENT_SOURCE_ACCOUNT_ID, event.getRequestContext().getAccountId());
                addAttribute(transaction, EVENT_SOURCE_API_ID, event.getRequestContext().getApiId());
                addAttribute(transaction, EVENT_SOURCE_STAGE, event.getRequestContext().getStage());
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from APIGatewayV2HTTPEvent");
        }
    }

    /**
     * Extracts metadata from CloudFrontEvent.
     * Adds event type only (no ARN available).
     */
    private static void extractCloudFrontMetadata(CloudFrontEvent event, Transaction transaction) {
        try {
            // Event type only
            addAttribute(transaction, EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE, EVENT_TYPE_CLOUDFRONT);
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINE, t, "Error extracting metadata from CloudFrontEvent");
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