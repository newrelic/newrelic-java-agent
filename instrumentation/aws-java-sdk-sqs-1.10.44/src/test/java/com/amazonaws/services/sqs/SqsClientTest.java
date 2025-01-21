/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.sqs;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import com.newrelic.utils.SqsV1Util;
import org.elasticmq.NodeAddress;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.amazonaws.services.sqs", "com.newrelic.utils" }, configName = "dt_enabled.yml")
public class SqsClientTest {

    private static AmazonSQSClient sqsClient;
    private static SQSRestServer server;
    private static String queueUrl;

    private static final String SCHEME = "http";
    private static final String HOST = "localhost";
    private static final String QUEUE_NAME = "myQueue";

    @BeforeClass
    public static void beforeClass() throws Exception {
        int port = InstrumentationTestRunner.getIntrospector().getRandomPort();
        server = SQSRestServerBuilder
                .withPort(port)
                .withInterface(HOST)
                .withServerAddress(new NodeAddress(SCHEME, HOST, port, ""))
                .start();
        server.waitUntilStarted();

        String endpoint = SCHEME + "://" + HOST + ":" + port;
        sqsClient = new AmazonSQSClient(new StaticCredentialsProvider(new BasicAWSCredentials("x", "x")));
        sqsClient.setEndpoint(endpoint);

        CreateQueueResult myQueue = sqsClient.createQueue((new CreateQueueRequest()).withQueueName(QUEUE_NAME));
        queueUrl = myQueue.getQueueUrl();
    }

    @AfterClass
    public static void afterClass() {
        server.stopAndWait();
    }

    @Test
    public void testSendMessage() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        SendMessageRequest request = sendMessageRequest();

        Set<String> dtHeaders = new HashSet<>(Arrays.asList(SqsV1Util.DT_HEADERS));
        boolean containsDtHeaders = request.getMessageAttributes().entrySet().stream().anyMatch(e ->
                dtHeaders.contains(e.getKey()) && e.getValue() != null && !e.getValue().getStringValue().isEmpty());
        assertTrue("Message request must contain headers", containsDtHeaders);

        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        checkScopedMetricCount(txName, "MessageBroker/SQS/Queue/Produce/Named/" + QUEUE_NAME, 1);
    }

    @Test
    public void testSendMessageBatch() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        SendMessageBatchRequest request = sendMessageBatch();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        Set<String> dtHeaders = new HashSet<>(Arrays.asList(SqsV1Util.DT_HEADERS));
        assertFalse("Batch request must contain at least one entry", request.getEntries().isEmpty());
        for (SendMessageBatchRequestEntry entry: request.getEntries()) {
            boolean containsDtHeaders = entry.getMessageAttributes().entrySet().stream().anyMatch(e ->
                    dtHeaders.contains(e.getKey()) && e.getValue() != null && !e.getValue().getStringValue().isEmpty());
            assertTrue("Message entry must contain headers", containsDtHeaders);
        }

        String txName = introspector.getTransactionNames().iterator().next();
        checkScopedMetricCount(txName, "MessageBroker/SQS/Queue/Produce/Named/" + QUEUE_NAME, 1);
    }

    @Test
    public void testReceiveMessage() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        receiveMessage();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        checkScopedMetricCount(txName, "MessageBroker/SQS/Queue/Consume/Named/" + QUEUE_NAME, 1);
    }

    @Trace(dispatcher = true)
    private SendMessageRequest sendMessageRequest() {
        SendMessageRequest request = (new SendMessageRequest()).withQueueUrl(queueUrl).withMessageBody("body");
        sqsClient.sendMessage(request);
        return request;
    }

    @Trace(dispatcher = true)
    private SendMessageBatchRequest sendMessageBatch() {
        SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
        SendMessageBatchRequest request = (new SendMessageBatchRequest()).withQueueUrl(queueUrl)
                .withEntries(entry);
        try {
            sqsClient.sendMessageBatch(request);
        } catch (Exception e) {
            // Do nothing
        }

        return request;
    }

    @Trace(dispatcher = true)
    private void receiveMessage() {
        ReceiveMessageRequest request = (new ReceiveMessageRequest()).withQueueUrl(queueUrl);
        sqsClient.receiveMessage(request);
    }

    public static void checkScopedMetricCount(String transactionName, String metricName, int expected) {
        TracedMetricData data = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(
                transactionName).get(metricName);
        assertEquals(expected, data.getCallCount());
    }
}
