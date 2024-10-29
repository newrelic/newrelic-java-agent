/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.lambda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws"}, configName = "dt_enabled.yml")
public class DefaultLambdaAsyncClient_InstrumentationTest {

    public AWSLambdaAsync lambdaClient;

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Before
    public void setup() throws URISyntaxException {
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(server.getEndPoint().toString(), "us-east-1");
        lambdaClient = AWSLambdaAsyncClient.asyncBuilder()
                .withEndpointConfiguration(endpoint)
                .withCredentials(new CredProvider())
                .build();
    }

    @Test
    public void testInvokeArn() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        txn();
        SpanEvent lambdaSpan = introspector.getSpanEvents().stream()
                .filter(span -> span.getName().equals("Lambda/invoke/my-function"))
                .findFirst().orElse(null);
        assertNotNull(lambdaSpan);
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", lambdaSpan.getAgentAttributes().get("cloud.resource_id"));

        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(
                "OtherTransaction/Custom/com.amazonaws.services.lambda.DefaultLambdaAsyncClient_InstrumentationTest/txn");
        assertEquals(1,transactionTraces.size());
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.get(0);
        assertEquals("Lambda/invoke/my-function", trace.getName());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", trace.getTracerAttributes().get("cloud.resource_id"));
    }

    @Trace(dispatcher = true)
    public void txn() {
        InvokeRequest request = new InvokeRequest();
        request.setFunctionName("arn:aws:lambda:us-east-1:123456789012:function:my-function");
        lambdaClient.invoke(request);
    }

    private static class CredProvider implements AWSCredentialsProvider {
        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {

                @Override
                public String getAWSAccessKeyId() {
                    return "accessKeyId";
                }

                @Override
                public String getAWSSecretKey() {
                    return "secretKey";
                }
            };
        }

        @Override
        public void refresh() {

        }
    }
}