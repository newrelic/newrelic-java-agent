/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda.runtime;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Instrumentation test for RequestHandler weaving.
 * Verifies that transactions are created and Lambda metadata is captured.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws.services.lambda.runtime"})
public class RequestHandlerInstrumentationTest {

    @Test
    public void testRequestHandlerCreatesTransaction() {
        Context mockContext = createMockContext();

        TestRequestHandler handler = new TestRequestHandler();
        String result = handler.handleRequest("test input", mockContext);

        assertEquals("processed: test input", result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());

        String transactionName = introspector.getTransactionNames().iterator().next();
        assertEquals("OtherTransaction/Function/test-function", transactionName);
    }

    @Test
    public void testRequestHandlerCapturesLambdaMetadata() {
        Context mockContext = createMockContext();

        TestRequestHandler handler = new TestRequestHandler();
        handler.handleRequest("test input", mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        TransactionEvent event = introspector.getTransactionEvents("OtherTransaction/Function/test-function").iterator().next();

        Map<String, Object> attributes = event.getAttributes();
        assertTrue("Should capture aws.lambda.arn", attributes.containsKey("aws.lambda.arn"));
        assertTrue("Should capture aws.lambda.function_version", attributes.containsKey("aws.lambda.function_version"));

        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function",
                     attributes.get("aws.lambda.arn"));
        assertEquals("$LATEST", attributes.get("aws.lambda.function_version"));
    }

    @Test
    public void testColdStartCaptured() {
        Context mockContext = createMockContext();

        TestRequestHandler handler1 = new TestRequestHandler();
        handler1.handleRequest("input1", mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        TransactionEvent firstEvent = introspector.getTransactionEvents("OtherTransaction/Function/test-function").iterator().next();
        Map<String, Object> firstAttributes = firstEvent.getAttributes();

        // Cold start should be true on first invocation
        assertEquals(true, firstAttributes.get("aws.lambda.coldStart"));

        // Clear introspector for second invocation
        introspector.clear();

        // Second invocation: should NOT be cold start
        TestRequestHandler handler2 = new TestRequestHandler();
        handler2.handleRequest("input2", mockContext);

        TransactionEvent secondEvent = introspector.getTransactionEvents("OtherTransaction/Function/test-function").iterator().next();
        Map<String, Object> secondAttributes = secondEvent.getAttributes();

        // Cold start should not be present or false on subsequent invocations
        // (The instrumentation only adds the attribute when it's true)
        if (secondAttributes.containsKey("aws.lambda.coldStart")) {
            assertEquals(false, secondAttributes.get("aws.lambda.coldStart"));
        }
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

    /**
     * Test implementation of RequestHandler for instrumentation testing.
     */
    public static class TestRequestHandler implements RequestHandler<String, String> {
        @Override
        public String handleRequest(String input, Context context) {
            return "processed: " + input;
        }
    }
}
