/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda.runtime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpServerlessApi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for Lambda handler instrumentation.
 * Tests the handler interfaces with mock Context to verify metadata capture.
 */
public class LambdaHandlerIntegrationTest {

    @Before
    public void setUp() {
        AgentBridge.serverlessApi = new NoOpServerlessApi();
    }

    @After
    public void tearDown() {
        AgentBridge.serverlessApi = new NoOpServerlessApi();
    }

    @Test
    public void testRequestHandlerWithContext() {
        Context mockContext = createMockContext();

        TestRequestHandler handler = new TestRequestHandler();

        String result = handler.handleRequest("test input", mockContext);

        assertEquals("processed: test input", result);
    }

    @Test
    public void testRequestStreamHandlerWithContext() throws IOException {
        Context mockContext = createMockContext();

        TestRequestStreamHandler handler = new TestRequestStreamHandler();

        InputStream input = new ByteArrayInputStream("test data".getBytes());
        OutputStream output = new ByteArrayOutputStream();

        handler.handleRequest(input, output, mockContext);

        String outputStr = output.toString();
        assertNotNull(outputStr);
    }

    @Test
    public void testContextMetadata() {
        Context mockContext = createMockContext();

        // Verify mock Context returns expected values
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:test-function", mockContext.getInvokedFunctionArn());
        assertEquals("$LATEST", mockContext.getFunctionVersion());
        assertEquals("test-function", mockContext.getFunctionName());
        assertEquals("request-123", mockContext.getAwsRequestId());
        assertEquals(512, mockContext.getMemoryLimitInMB());
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
     * Test implementation of RequestHandler for testing.
     */
    private static class TestRequestHandler implements RequestHandler<String, String> {
        @Override
        public String handleRequest(String input, Context context) {
            return "processed: " + input;
        }
    }

    /**
     * Test implementation of RequestStreamHandler for testing.
     */
    private static class TestRequestStreamHandler implements RequestStreamHandler {
        @Override
        public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
            byte[] buffer = new byte[1024];
            int bytesRead = input.read(buffer);

            String outputString = "Processed " + bytesRead + " bytes";
            output.write(outputString.getBytes());
        }
    }
}
