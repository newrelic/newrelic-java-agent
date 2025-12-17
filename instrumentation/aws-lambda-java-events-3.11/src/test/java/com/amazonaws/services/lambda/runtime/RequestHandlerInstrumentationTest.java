/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda.runtime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Instrumentation test for RequestHandler weaving.
 * Verifies that transactions are created and named correctly.
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

        // Verify serverless metadata was captured via AgentBridge
        assertNotNull("ARN should be captured", AgentBridge.serverlessApi.getArn());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function", AgentBridge.serverlessApi.getArn());
        assertNotNull("Function version should be captured", AgentBridge.serverlessApi.getFunctionVersion());
        assertEquals("$LATEST", AgentBridge.serverlessApi.getFunctionVersion());
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
