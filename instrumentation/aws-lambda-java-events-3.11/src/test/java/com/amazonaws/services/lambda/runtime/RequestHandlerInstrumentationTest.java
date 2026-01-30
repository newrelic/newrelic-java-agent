/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda.runtime;

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
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.nr.instrumentation.lambda.LambdaInstrumentationHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.nr.instrumentation.lambda.LambdaConstants.AWS_REQUEST_ID_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_ARN_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.LAMBDA_ARN_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaConstants.LAMBDA_COLD_START_ATTRIBUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Instrumentation test for RequestHandler weaving.
 * Verifies that transactions are created and named correctly.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws.services.lambda.runtime"})
public class RequestHandlerInstrumentationTest {

    @Before
    public void setUp() {
        LambdaInstrumentationHelper.resetColdStartForTesting();
    }

    @Test
    public void testRequestHandlerCreatesTransaction() {
        Context mockContext = createMockContext();

        TestRequestHandler handler = new TestRequestHandler();
        String result = handler.handleRequest("test input", mockContext);

        assertEquals("processed: test input", result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());

        // Verify serverless metadata was captured via AgentBridge
        assertNotNull("ARN should be captured", AgentBridge.serverlessApi.getArn());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function", AgentBridge.serverlessApi.getArn());
        assertNotNull("Function version should be captured", AgentBridge.serverlessApi.getFunctionVersion());
        assertEquals("$LATEST", AgentBridge.serverlessApi.getFunctionVersion());

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestRequestHandler/handleRequest");
        assertEquals("Expected exactly one transaction event", 1, transactionEvents.size());

        TransactionEvent event = transactionEvents.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("aws.lambda.arn attribute should be present", attributes.containsKey(LAMBDA_ARN_ATTRIBUTE));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function", attributes.get(LAMBDA_ARN_ATTRIBUTE));

        assertTrue("aws.requestId attribute should be present", attributes.containsKey(AWS_REQUEST_ID_ATTRIBUTE));
        assertEquals("request-123", attributes.get(AWS_REQUEST_ID_ATTRIBUTE));
    }

    @Test
    public void testRequestHandlerWithContextCreatesTransaction() {
        Context mockContext = createMockContext();

        TestRequestHandler handler = new TestRequestHandler();
        handler.handleRequest("test input", mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());

        // Verify serverless metadata was captured
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function", AgentBridge.serverlessApi.getArn());
        assertEquals("$LATEST", AgentBridge.serverlessApi.getFunctionVersion());

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestRequestHandler/handleRequest");
        assertEquals("Expected exactly one transaction event", 1, transactionEvents.size());

        TransactionEvent event = transactionEvents.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("aws.lambda.arn attribute should be present", attributes.containsKey(LAMBDA_ARN_ATTRIBUTE));
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function", attributes.get(LAMBDA_ARN_ATTRIBUTE));

        assertTrue("aws.requestId attribute should be present", attributes.containsKey(AWS_REQUEST_ID_ATTRIBUTE));
        assertEquals("request-123", attributes.get(AWS_REQUEST_ID_ATTRIBUTE));
    }

    @Test
    public void testMultipleInvocations() {
        Context mockContext = createMockContext();

        // First invocation
        TestRequestHandler handler1 = new TestRequestHandler();
        handler1.handleRequest("input1", mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction after first invocation", 1, introspector.getFinishedTransactionCount());

        // Second invocation
        TestRequestHandler handler2 = new TestRequestHandler();
        handler2.handleRequest("input2", mockContext);

        // Verify second transaction was created
        assertEquals("Expected two transactions after second invocation", 2, introspector.getFinishedTransactionCount());
    }

    @Test
    public void testRequestHandlerWithNullContext() {
        // Test that null context doesn't break instrumentation
        TestRequestHandler handler = new TestRequestHandler();
        String result = handler.handleRequest("test input", null);

        assertEquals("processed: test input", result);

        // Verify transaction was still created (even without metadata)
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());
    }

    @Test
    public void testColdStartAttributeOnFirstInvocation() {
        Context mockContext = createMockContext();

        TestRequestHandler handler = new TestRequestHandler();
        handler.handleRequest("first invocation", mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestRequestHandler/handleRequest");
        assertEquals("Expected exactly one transaction event", 1, transactionEvents.size());

        TransactionEvent event = transactionEvents.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Cold start attribute should be present", attributes.containsKey(LAMBDA_COLD_START_ATTRIBUTE));
        assertEquals("Cold start attribute should be true on first invocation", true, attributes.get(LAMBDA_COLD_START_ATTRIBUTE));
    }

    @Test
    public void testColdStartAttributeNotPresentOnSubsequentInvocations() {
        Context mockContext = createMockContext();

        TestRequestHandler handler1 = new TestRequestHandler();
        handler1.handleRequest("first invocation", mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected one transaction after first invocation", 1, introspector.getFinishedTransactionCount());

        TestRequestHandler handler2 = new TestRequestHandler();
        handler2.handleRequest("second invocation", mockContext);

        assertEquals("Expected two transactions after second invocation", 2, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> allEvents = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestRequestHandler/handleRequest");
        assertEquals("Expected exactly two transaction events", 2, allEvents.size());

        // Count how many events have the cold start attribute
        // According to the spec, only the first invocation should have it
        int coldStartCount = 0;
        for (TransactionEvent event : allEvents) {
            Map<String, Object> attributes = event.getAttributes();
            if (attributes.containsKey(LAMBDA_COLD_START_ATTRIBUTE)) {
                coldStartCount++;
                assertEquals("Cold start attribute should be true when present", true, attributes.get(LAMBDA_COLD_START_ATTRIBUTE));
            }
        }

        assertEquals("Exactly one transaction should have cold start attribute", 1, coldStartCount);
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
        when(context.getLogStreamName()).thenReturn("2025/12/12/[$LATEST]abc123");
        return context;
    }

    @Test
    public void testS3EventSourceArn() {
        Context mockContext = createMockContext();

        S3EventNotification.S3BucketEntity bucket = mock(S3EventNotification.S3BucketEntity.class);
        when(bucket.getArn()).thenReturn("arn:aws:s3:::my-bucket");

        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        when(s3Entity.getBucket()).thenReturn(bucket);

        S3EventNotification.S3EventNotificationRecord record = mock(S3EventNotification.S3EventNotificationRecord.class);
        when(record.getS3()).thenReturn(s3Entity);

        S3Event s3Event = new S3Event(Collections.singletonList(record));

        TestS3Handler handler = new TestS3Handler();
        handler.handleRequest(s3Event, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestS3Handler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:s3:::my-bucket", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("s3", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testSNSEventSourceArn() {
        Context mockContext = createMockContext();

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setEventSubscriptionArn("arn:aws:sns:us-east-1:123456789012:my-topic");

        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(Collections.singletonList(record));

        TestSNSHandler handler = new TestSNSHandler();
        handler.handleRequest(snsEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestSNSHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:sns:us-east-1:123456789012:my-topic", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("sns", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testSQSEventSourceArn() {
        Context mockContext = createMockContext();

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:my-queue");

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Collections.singletonList(message));

        TestSQSHandler handler = new TestSQSHandler();
        handler.handleRequest(sqsEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestSQSHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:sqs:us-east-1:123456789012:my-queue", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("sqs", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testDynamodbEventSourceArn() {
        Context mockContext = createMockContext();

        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setEventSourceARN("arn:aws:dynamodb:us-east-1:123456789012:table/my-table/stream/2021-01-01T00:00:00.000");

        DynamodbEvent dynamodbEvent = new DynamodbEvent();
        dynamodbEvent.setRecords(Collections.singletonList(record));

        TestDynamodbHandler handler = new TestDynamodbHandler();
        handler.handleRequest(dynamodbEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestDynamodbHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:dynamodb:us-east-1:123456789012:table/my-table/stream/2021-01-01T00:00:00.000", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("dynamo_streams", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testKinesisEventSourceArn() {
        Context mockContext = createMockContext();

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setEventSourceARN("arn:aws:kinesis:us-east-1:123456789012:stream/my-stream");

        KinesisEvent kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(Collections.singletonList(record));

        TestKinesisHandler handler = new TestKinesisHandler();
        handler.handleRequest(kinesisEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestKinesisHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:kinesis:us-east-1:123456789012:stream/my-stream", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("kinesis", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testKinesisFirehoseEventSourceArn() {
        Context mockContext = createMockContext();

        KinesisFirehoseEvent firehoseEvent = new KinesisFirehoseEvent();
        firehoseEvent.setDeliveryStreamArn("arn:aws:firehose:us-east-1:123456789012:deliverystream/my-stream");

        TestKinesisFirehoseHandler handler = new TestKinesisFirehoseHandler();
        handler.handleRequest(firehoseEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestKinesisFirehoseHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:firehose:us-east-1:123456789012:deliverystream/my-stream", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("firehose", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testCodeCommitEventSourceArn() {
        Context mockContext = createMockContext();

        CodeCommitEvent.Record record = new CodeCommitEvent.Record();
        record.setEventSourceArn("arn:aws:codecommit:us-east-1:123456789012:my-repo");

        CodeCommitEvent codeCommitEvent = new CodeCommitEvent();
        codeCommitEvent.setRecords(Collections.singletonList(record));

        TestCodeCommitHandler handler = new TestCodeCommitHandler();
        handler.handleRequest(codeCommitEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestCodeCommitHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:codecommit:us-east-1:123456789012:my-repo", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        // CodeCommit is not in the spec, so eventType should NOT be present
        assertFalse("Event source event type should NOT be present for CodeCommit", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testScheduledEventSourceArn() {
        Context mockContext = createMockContext();

        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setResources(Collections.singletonList("arn:aws:events:us-east-1:123456789012:rule/my-rule"));

        TestScheduledHandler handler = new TestScheduledHandler();
        handler.handleRequest(scheduledEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestScheduledHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:events:us-east-1:123456789012:rule/my-rule", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("cloudWatch_scheduled", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testALBEventSourceArn() {
        Context mockContext = createMockContext();

        ApplicationLoadBalancerRequestEvent.Elb elb = new ApplicationLoadBalancerRequestEvent.Elb();
        elb.setTargetGroupArn("arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/my-targets/12345");

        ApplicationLoadBalancerRequestEvent.RequestContext requestContext = new ApplicationLoadBalancerRequestEvent.RequestContext();
        requestContext.setElb(elb);

        ApplicationLoadBalancerRequestEvent albEvent = new ApplicationLoadBalancerRequestEvent();
        albEvent.setRequestContext(requestContext);

        TestALBHandler handler = new TestALBHandler();
        handler.handleRequest(albEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestALBHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/my-targets/12345", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("alb", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testAPIGatewayEventType() {
        Context mockContext = createMockContext();

        APIGatewayProxyRequestEvent apiGatewayEvent = new APIGatewayProxyRequestEvent();

        TestAPIGatewayHandler handler = new TestAPIGatewayHandler();
        handler.handleRequest(apiGatewayEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestAPIGatewayHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // API Gateway has no ARN, only eventType
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("apiGateway", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testAPIGatewayV2HTTPEventType() {
        Context mockContext = createMockContext();

        APIGatewayV2HTTPEvent apiGatewayV2Event = new APIGatewayV2HTTPEvent();

        TestAPIGatewayV2HTTPHandler handler = new TestAPIGatewayV2HTTPHandler();
        handler.handleRequest(apiGatewayV2Event, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestAPIGatewayV2HTTPHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // API Gateway V2 HTTP has no ARN, only eventType (same as V1)
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("apiGateway", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    @Test
    public void testCloudFrontEventType() {
        Context mockContext = createMockContext();

        CloudFrontEvent cloudFrontEvent = new CloudFrontEvent();

        TestCloudFrontHandler handler = new TestCloudFrontHandler();
        handler.handleRequest(cloudFrontEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents("OtherTransaction/Java/com.amazonaws.services.lambda.runtime.RequestHandlerInstrumentationTest$TestCloudFrontHandler/handleRequest");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // CloudFront has no ARN, only eventType
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("cloudFront", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
    }

    /**
     * Test implementation of RequestHandler for instrumentation testing.
     */
    public static class TestRequestHandler implements RequestHandler<String, String> {
        @Override
        public String handleRequest(String input, Context context) {
            return "processed: " + input;
        }
    }

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

    public static class TestDynamodbHandler implements RequestHandler<DynamodbEvent, Void> {
        @Override
        public Void handleRequest(DynamodbEvent event, Context context) {
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

    public static class TestCodeCommitHandler implements RequestHandler<CodeCommitEvent, Void> {
        @Override
        public Void handleRequest(CodeCommitEvent event, Context context) {
            return null;
        }
    }

    public static class TestScheduledHandler implements RequestHandler<ScheduledEvent, Void> {
        @Override
        public Void handleRequest(ScheduledEvent event, Context context) {
            return null;
        }
    }

    public static class TestALBHandler implements RequestHandler<ApplicationLoadBalancerRequestEvent, String> {
        @Override
        public String handleRequest(ApplicationLoadBalancerRequestEvent event, Context context) {
            return "ok";
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

    public static class TestCloudFrontHandler implements RequestHandler<CloudFrontEvent, String> {
        @Override
        public String handleRequest(CloudFrontEvent event, Context context) {
            return "ok";
        }
    }
}