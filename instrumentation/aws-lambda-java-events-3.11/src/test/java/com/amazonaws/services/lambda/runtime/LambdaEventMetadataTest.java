/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda.runtime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.nr.instrumentation.lambda.LambdaInstrumentationHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.joda.time.DateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.nr.instrumentation.lambda.LambdaConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for event-specific metadata extraction in Lambda instrumentation.
 * Verifies that event-specific attributes are captured correctly per the Lambda spec.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws.services.lambda.runtime"})
public class LambdaEventMetadataTest {

    @Before
    public void setUp() {
        LambdaInstrumentationHelper.resetColdStartForTesting();
    }

    @Test
    public void testS3EventMetadata() {
        Context mockContext = createMockContext();

        // Create S3 event with full metadata
        S3EventNotification.UserIdentityEntity userIdentity = mock(S3EventNotification.UserIdentityEntity.class);
        when(userIdentity.getPrincipalId()).thenReturn("AIDAI123456789");

        S3EventNotification.RequestParametersEntity requestParameters = mock(S3EventNotification.RequestParametersEntity.class);
        when(requestParameters.getSourceIPAddress()).thenReturn("192.168.1.1");

        S3EventNotification.ResponseElementsEntity responseElements = mock(S3EventNotification.ResponseElementsEntity.class);
        when(responseElements.getxAmzId2()).thenReturn("xAmzId2Value");
        when(responseElements.getxAmzRequestId()).thenReturn("xAmzRequestIdValue");

        S3EventNotification.S3BucketEntity bucket = mock(S3EventNotification.S3BucketEntity.class);
        when(bucket.getName()).thenReturn("my-test-bucket");
        when(bucket.getArn()).thenReturn("arn:aws:s3:::my-test-bucket");

        S3EventNotification.S3ObjectEntity object = mock(S3EventNotification.S3ObjectEntity.class);
        when(object.getKey()).thenReturn("test-folder/test-file.txt");
        when(object.getSizeAsLong()).thenReturn(1024L);
        when(object.getSequencer()).thenReturn("0055AED6DCD90281E5");

        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        when(s3Entity.getBucket()).thenReturn(bucket);
        when(s3Entity.getObject()).thenReturn(object);
        when(s3Entity.getConfigurationId()).thenReturn("testConfigRule");

        S3EventNotification.S3EventNotificationRecord record = mock(S3EventNotification.S3EventNotificationRecord.class);
        when(record.getEventName()).thenReturn("ObjectCreated:Put");
        when(record.getEventTime()).thenReturn(DateTime.parse("2021-01-01T12:00:00.000Z"));
        when(record.getAwsRegion()).thenReturn("us-east-1");
        when(record.getUserIdentity()).thenReturn(userIdentity);
        when(record.getRequestParameters()).thenReturn(requestParameters);
        when(record.getResponseElements()).thenReturn(responseElements);
        when(record.getS3()).thenReturn(s3Entity);

        S3Event s3Event = new S3Event(Collections.singletonList(record));

        TestS3Handler handler = new TestS3Handler();
        handler.handleRequest(s3Event, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestS3Handler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify S3-specific metadata
        assertTrue("Event source length should be present", attributes.containsKey(EVENT_SOURCE_LENGTH));
        assertEquals(1, attributes.get(EVENT_SOURCE_LENGTH));

        assertTrue("Event source region should be present", attributes.containsKey(EVENT_SOURCE_REGION));
        assertEquals("us-east-1", attributes.get(EVENT_SOURCE_REGION));

        assertTrue("Event name should be present", attributes.containsKey(EVENT_SOURCE_EVENT_NAME));
        assertEquals("ObjectCreated:Put", attributes.get(EVENT_SOURCE_EVENT_NAME));

        assertTrue("Event time should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TIME));
        assertEquals("2021-01-01T12:00:00.000Z", attributes.get(EVENT_SOURCE_EVENT_TIME));

        assertTrue("xAmzId2 should be present", attributes.containsKey(EVENT_SOURCE_X_AMZ_ID_2));
        assertEquals("xAmzId2Value", attributes.get(EVENT_SOURCE_X_AMZ_ID_2));

        assertTrue("Bucket name should be present", attributes.containsKey(EVENT_SOURCE_BUCKET_NAME));
        assertEquals("my-test-bucket", attributes.get(EVENT_SOURCE_BUCKET_NAME));

        assertTrue("Object key should be present", attributes.containsKey(EVENT_SOURCE_OBJECT_KEY));
        assertEquals("test-folder/test-file.txt", attributes.get(EVENT_SOURCE_OBJECT_KEY));

        assertTrue("Object sequencer should be present", attributes.containsKey(EVENT_SOURCE_OBJECT_SEQUENCER));
        assertEquals("0055AED6DCD90281E5", attributes.get(EVENT_SOURCE_OBJECT_SEQUENCER));

        assertTrue("Object size should be present", attributes.containsKey(EVENT_SOURCE_OBJECT_SIZE));
        assertEquals(1024L, attributes.get(EVENT_SOURCE_OBJECT_SIZE));
    }

    @Test
    public void testSNSEventMetadata() {
        Context mockContext = createMockContext();

        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessageId("12345678-1234-1234-1234-123456789012");
        sns.setTimestamp(DateTime.parse("2021-01-01T12:00:00.000Z"));
        sns.setTopicArn("arn:aws:sns:us-east-1:123456789012:my-topic");
        sns.setType("Notification");

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);
        record.setEventSubscriptionArn("arn:aws:sns:us-east-1:123456789012:my-topic:subscription-id");

        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(Collections.singletonList(record));

        TestSNSHandler handler = new TestSNSHandler();
        handler.handleRequest(snsEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestSNSHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify SNS-specific metadata
        assertTrue("Event source length should be present", attributes.containsKey(EVENT_SOURCE_LENGTH));
        assertEquals(1, attributes.get(EVENT_SOURCE_LENGTH));

        assertTrue("Message ID should be present", attributes.containsKey(EVENT_SOURCE_MESSAGE_ID));
        assertEquals("12345678-1234-1234-1234-123456789012", attributes.get(EVENT_SOURCE_MESSAGE_ID));

        assertTrue("Timestamp should be present", attributes.containsKey(EVENT_SOURCE_TIMESTAMP));
        assertEquals("2021-01-01T12:00:00.000Z", attributes.get(EVENT_SOURCE_TIMESTAMP));

        assertTrue("Topic ARN should be present", attributes.containsKey(EVENT_SOURCE_TOPIC_ARN));
        assertEquals("arn:aws:sns:us-east-1:123456789012:my-topic", attributes.get(EVENT_SOURCE_TOPIC_ARN));

        assertTrue("Type should be present", attributes.containsKey(EVENT_SOURCE_TYPE));
        assertEquals("Notification", attributes.get(EVENT_SOURCE_TYPE));
    }

    @Test
    public void testSQSEventMetadata() {
        Context mockContext = createMockContext();

        SQSEvent.SQSMessage message1 = new SQSEvent.SQSMessage();
        message1.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:my-queue");

        SQSEvent.SQSMessage message2 = new SQSEvent.SQSMessage();
        message2.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:my-queue");

        SQSEvent.SQSMessage message3 = new SQSEvent.SQSMessage();
        message3.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:my-queue");

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(java.util.Arrays.asList(message1, message2, message3));

        TestSQSHandler handler = new TestSQSHandler();
        handler.handleRequest(sqsEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestSQSHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify SQS-specific metadata (length only)
        assertTrue("Event source length should be present", attributes.containsKey(EVENT_SOURCE_LENGTH));
        assertEquals(3, attributes.get(EVENT_SOURCE_LENGTH));
    }

    @Test
    public void testKinesisEventMetadata() {
        Context mockContext = createMockContext();

        KinesisEvent.KinesisEventRecord record1 = new KinesisEvent.KinesisEventRecord();
        record1.setEventSourceARN("arn:aws:kinesis:us-west-2:123456789012:stream/my-stream");
        record1.setAwsRegion("us-west-2");

        KinesisEvent.KinesisEventRecord record2 = new KinesisEvent.KinesisEventRecord();
        record2.setEventSourceARN("arn:aws:kinesis:us-west-2:123456789012:stream/my-stream");
        record2.setAwsRegion("us-west-2");

        KinesisEvent kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(java.util.Arrays.asList(record1, record2));

        TestKinesisHandler handler = new TestKinesisHandler();
        handler.handleRequest(kinesisEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestKinesisHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify Kinesis-specific metadata
        assertTrue("Event source length should be present", attributes.containsKey(EVENT_SOURCE_LENGTH));
        assertEquals(2, attributes.get(EVENT_SOURCE_LENGTH));

        assertTrue("Event source region should be present", attributes.containsKey(EVENT_SOURCE_REGION));
        assertEquals("us-west-2", attributes.get(EVENT_SOURCE_REGION));
    }

    @Test
    public void testKinesisFirehoseEventMetadata() {
        Context mockContext = createMockContext();

        KinesisFirehoseEvent.Record record1 = new KinesisFirehoseEvent.Record();
        KinesisFirehoseEvent.Record record2 = new KinesisFirehoseEvent.Record();
        KinesisFirehoseEvent.Record record3 = new KinesisFirehoseEvent.Record();

        KinesisFirehoseEvent firehoseEvent = new KinesisFirehoseEvent();
        firehoseEvent.setDeliveryStreamArn("arn:aws:firehose:eu-west-1:123456789012:deliverystream/my-stream");
        firehoseEvent.setRegion("eu-west-1");
        firehoseEvent.setRecords(java.util.Arrays.asList(record1, record2, record3));

        TestKinesisFirehoseHandler handler = new TestKinesisFirehoseHandler();
        handler.handleRequest(firehoseEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestKinesisFirehoseHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify Kinesis Firehose-specific metadata
        assertTrue("Event source length should be present", attributes.containsKey(EVENT_SOURCE_LENGTH));
        assertEquals(3, attributes.get(EVENT_SOURCE_LENGTH));

        assertTrue("Event source region should be present", attributes.containsKey(EVENT_SOURCE_REGION));
        assertEquals("eu-west-1", attributes.get(EVENT_SOURCE_REGION));
    }

    @Test
    public void testScheduledEventMetadata() {
        Context mockContext = createMockContext();

        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setAccount("123456789012");
        scheduledEvent.setId("cdc73f9d-aea9-11e3-9d5a-835b769c0d9c");
        scheduledEvent.setRegion("us-east-1");
        scheduledEvent.setResources(Collections.singletonList("arn:aws:events:us-east-1:123456789012:rule/my-scheduled-rule"));
        scheduledEvent.setTime(DateTime.parse("2021-01-01T12:00:00.000Z"));

        TestScheduledHandler handler = new TestScheduledHandler();
        handler.handleRequest(scheduledEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestScheduledHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify CloudWatch Scheduled-specific metadata
        assertTrue("Account should be present", attributes.containsKey(EVENT_SOURCE_ACCOUNT));
        assertEquals("123456789012", attributes.get(EVENT_SOURCE_ACCOUNT));

        assertTrue("ID should be present", attributes.containsKey(EVENT_SOURCE_ID));
        assertEquals("cdc73f9d-aea9-11e3-9d5a-835b769c0d9c", attributes.get(EVENT_SOURCE_ID));

        assertTrue("Region should be present", attributes.containsKey(EVENT_SOURCE_REGION));
        assertEquals("us-east-1", attributes.get(EVENT_SOURCE_REGION));

        assertTrue("Resource should be present", attributes.containsKey(EVENT_SOURCE_RESOURCE));
        assertEquals("arn:aws:events:us-east-1:123456789012:rule/my-scheduled-rule", attributes.get(EVENT_SOURCE_RESOURCE));

        assertTrue("Time should be present", attributes.containsKey(EVENT_SOURCE_TIME));
        assertEquals("2021-01-01T12:00:00.000Z", attributes.get(EVENT_SOURCE_TIME));
    }

    @Test
    public void testAPIGatewayProxyEventMetadata() {
        Context mockContext = createMockContext();

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setAccountId("123456789012");
        requestContext.setApiId("abcd1234");
        requestContext.setResourceId("xyz789");
        requestContext.setResourcePath("/users/{id}");
        requestContext.setStage("prod");

        APIGatewayProxyRequestEvent apiGatewayEvent = new APIGatewayProxyRequestEvent();
        apiGatewayEvent.setRequestContext(requestContext);

        TestAPIGatewayHandler handler = new TestAPIGatewayHandler();
        handler.handleRequest(apiGatewayEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestAPIGatewayHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify API Gateway V1-specific metadata
        assertTrue("Account ID should be present", attributes.containsKey(EVENT_SOURCE_ACCOUNT_ID));
        assertEquals("123456789012", attributes.get(EVENT_SOURCE_ACCOUNT_ID));

        assertTrue("API ID should be present", attributes.containsKey(EVENT_SOURCE_API_ID));
        assertEquals("abcd1234", attributes.get(EVENT_SOURCE_API_ID));

        assertTrue("Resource ID should be present", attributes.containsKey(EVENT_SOURCE_RESOURCE_ID));
        assertEquals("xyz789", attributes.get(EVENT_SOURCE_RESOURCE_ID));

        assertTrue("Resource path should be present", attributes.containsKey(EVENT_SOURCE_RESOURCE_PATH));
        assertEquals("/users/{id}", attributes.get(EVENT_SOURCE_RESOURCE_PATH));

        assertTrue("Stage should be present", attributes.containsKey(EVENT_SOURCE_STAGE));
        assertEquals("prod", attributes.get(EVENT_SOURCE_STAGE));
    }

    @Test
    public void testAPIGatewayV2HTTPEventMetadata() {
        Context mockContext = createMockContext();

        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        requestContext.setAccountId("987654321098");
        requestContext.setApiId("v2api123");
        requestContext.setStage("beta");

        APIGatewayV2HTTPEvent apiGatewayV2Event = new APIGatewayV2HTTPEvent();
        apiGatewayV2Event.setRequestContext(requestContext);

        TestAPIGatewayV2HTTPHandler handler = new TestAPIGatewayV2HTTPHandler();
        handler.handleRequest(apiGatewayV2Event, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Java/com.amazonaws.services.lambda.runtime.LambdaEventMetadataTest$TestAPIGatewayV2HTTPHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify API Gateway V2-specific metadata (no resourceId or resourcePath)
        assertTrue("Account ID should be present", attributes.containsKey(EVENT_SOURCE_ACCOUNT_ID));
        assertEquals("987654321098", attributes.get(EVENT_SOURCE_ACCOUNT_ID));

        assertTrue("API ID should be present", attributes.containsKey(EVENT_SOURCE_API_ID));
        assertEquals("v2api123", attributes.get(EVENT_SOURCE_API_ID));

        assertTrue("Stage should be present", attributes.containsKey(EVENT_SOURCE_STAGE));
        assertEquals("beta", attributes.get(EVENT_SOURCE_STAGE));

        // V2 should NOT have resourceId or resourcePath
        assertTrue("Resource ID should NOT be present for V2",
                !attributes.containsKey(EVENT_SOURCE_RESOURCE_ID) || attributes.get(EVENT_SOURCE_RESOURCE_ID) == null);
        assertTrue("Resource path should NOT be present for V2",
                !attributes.containsKey(EVENT_SOURCE_RESOURCE_PATH) || attributes.get(EVENT_SOURCE_RESOURCE_PATH) == null);
    }

    /**
     * Creates a mock Lambda Context for testing.
     */
    private Context createMockContext() {
        Context context = mock(Context.class);
        when(context.getInvokedFunctionArn()).thenReturn("arn:aws:lambda:us-east-1:123456789012:function:test-function");
        when(context.getFunctionVersion()).thenReturn("$LATEST");
        when(context.getFunctionName()).thenReturn("test-function");
        when(context.getAwsRequestId()).thenReturn("request-123");
        when(context.getMemoryLimitInMB()).thenReturn(512);
        when(context.getRemainingTimeInMillis()).thenReturn(30000);
        when(context.getLogGroupName()).thenReturn("/aws/lambda/test-function");
        when(context.getLogStreamName()).thenReturn("2026/01/01/[$LATEST]abc123");
        return context;
    }

    // Test handler implementations
    public static class TestS3Handler implements RequestHandler<S3Event, Void> {
        @Override
        public Void handleRequest(S3Event event, Context context) {
            return null;
        }
    }

    public static class TestSNSHandler implements RequestHandler<SNSEvent, Void> {
        @Override
        public Void handleRequest(SNSEvent event, Context context) {
            return null;
        }
    }

    public static class TestSQSHandler implements RequestHandler<SQSEvent, Void> {
        @Override
        public Void handleRequest(SQSEvent event, Context context) {
            return null;
        }
    }

    public static class TestKinesisHandler implements RequestHandler<KinesisEvent, Void> {
        @Override
        public Void handleRequest(KinesisEvent event, Context context) {
            return null;
        }
    }

    public static class TestKinesisFirehoseHandler implements RequestHandler<KinesisFirehoseEvent, KinesisFirehoseEvent> {
        @Override
        public KinesisFirehoseEvent handleRequest(KinesisFirehoseEvent event, Context context) {
            return event;
        }
    }

    public static class TestScheduledHandler implements RequestHandler<ScheduledEvent, Void> {
        @Override
        public Void handleRequest(ScheduledEvent event, Context context) {
            return null;
        }
    }

    public static class TestAPIGatewayHandler implements RequestHandler<APIGatewayProxyRequestEvent, String> {
        @Override
        public String handleRequest(APIGatewayProxyRequestEvent event, Context context) {
            return "ok";
        }
    }

    public static class TestAPIGatewayV2HTTPHandler implements RequestHandler<APIGatewayV2HTTPEvent, String> {
        @Override
        public String handleRequest(APIGatewayV2HTTPEvent event, Context context) {
            return "ok";
        }
    }
}
