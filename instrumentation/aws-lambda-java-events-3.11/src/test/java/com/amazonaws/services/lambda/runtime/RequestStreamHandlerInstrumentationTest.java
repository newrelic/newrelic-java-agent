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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Instrumentation test for RequestStreamHandler weaving.
 * Verifies that transactions are created and Lambda metadata is captured for stream-based handlers.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws.services.lambda.runtime"})
public class RequestStreamHandlerInstrumentationTest {

    @Test
    public void testRequestStreamHandlerCreatesTransaction() throws IOException {
        Context mockContext = createMockContext();

        TestRequestStreamHandler handler = new TestRequestStreamHandler();

        InputStream input = new ByteArrayInputStream("test data".getBytes());
        OutputStream output = new ByteArrayOutputStream();

        handler.handleRequest(input, output, mockContext);

        String outputStr = output.toString();
        assertNotNull(outputStr);
        assertTrue("Output should contain processed data", outputStr.contains("Processed"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());

        String transactionName = introspector.getTransactionNames().iterator().next();
        assertEquals("OtherTransaction/Function/stream-function", transactionName);
    }

    @Test
    public void testRequestStreamHandlerCapturesMetadata() throws IOException {
        Context mockContext = createMockContext();

        TestRequestStreamHandler handler = new TestRequestStreamHandler();
        InputStream input = new ByteArrayInputStream("test data".getBytes());
        OutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(input, output, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        TransactionEvent event = introspector.getTransactionEvents("OtherTransaction/Function/stream-function").iterator().next();

        Map<String, Object> attributes = event.getAttributes();
        assertTrue("Should capture aws.lambda.arn", attributes.containsKey("aws.lambda.arn"));
        assertTrue("Should capture aws.lambda.function_version", attributes.containsKey("aws.lambda.function_version"));

        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:stream-function",
                     attributes.get("aws.lambda.arn"));
        assertEquals("v2", attributes.get("aws.lambda.function_version"));
    }

    @Test
    public void testRequestStreamHandlerWithNullContext() throws IOException {
        // Test that null context doesn't break instrumentation
        TestRequestStreamHandler handler = new TestRequestStreamHandler();
        InputStream input = new ByteArrayInputStream("test data".getBytes());
        OutputStream output = new ByteArrayOutputStream();

        handler.handleRequest(input, output, null);

        // Verify handler still executes
        String outputStr = output.toString();
        assertNotNull(outputStr);

        // Verify transaction was still created
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected exactly one transaction", 1, introspector.getFinishedTransactionCount());
    }

    @Test
    public void testMultipleStreamHandlerInvocations() throws IOException {
        Context mockContext = createMockContext();

        TestRequestStreamHandler handler1 = new TestRequestStreamHandler();
        InputStream input1 = new ByteArrayInputStream("data1".getBytes());
        OutputStream output1 = new ByteArrayOutputStream();
        handler1.handleRequest(input1, output1, mockContext);

        TestRequestStreamHandler handler2 = new TestRequestStreamHandler();
        InputStream input2 = new ByteArrayInputStream("data2".getBytes());
        OutputStream output2 = new ByteArrayOutputStream();
        handler2.handleRequest(input2, output2, mockContext);

        // Verify two separate transactions were created
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Expected two transactions", 2, introspector.getFinishedTransactionCount());
    }

    /**
     * Creates a mock Lambda Context for testing.
     */
    private Context createMockContext() {
        Context context = mock(Context.class);
        when(context.getInvokedFunctionArn()).thenReturn("arn:aws:lambda:us-east-1:123456789012:function:stream-function");
        when(context.getFunctionVersion()).thenReturn("v2");
        when(context.getFunctionName()).thenReturn("stream-function");
        when(context.getAwsRequestId()).thenReturn("stream-request-456");
        when(context.getMemoryLimitInMB()).thenReturn(1024);
        when(context.getRemainingTimeInMillis()).thenReturn(60000);
        when(context.getLogGroupName()).thenReturn("/aws/lambda/stream-function");
        when(context.getLogStreamName()).thenReturn("2025/12/12/[v2]def456");
        return context;
    }

    /**
     * Test implementation of RequestStreamHandler for instrumentation testing.
     */
    public static class TestRequestStreamHandler implements RequestStreamHandler {
        @Override
        public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
            byte[] buffer = new byte[1024];
            int bytesRead = input.read(buffer);

            String outputString = "Processed " + bytesRead + " bytes";
            output.write(outputString.getBytes());
        }
    }
}
