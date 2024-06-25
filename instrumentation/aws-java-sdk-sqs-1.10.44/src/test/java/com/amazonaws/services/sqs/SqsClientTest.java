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
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import org.elasticmq.NodeAddress;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.amazonaws.services.sqs" }, configName = "dt_enabled.yml")
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
        sendMessageRequest();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        checkScopedMetricCount(txName, "MessageBroker/SQS/Queue/Produce/Named/" + QUEUE_NAME, 1);
    }

    @Test
    public void testSendMessageBatch() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        sendMessageBatch();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

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
    private void sendMessageRequest() {
        SendMessageRequest request = (new SendMessageRequest()).withQueueUrl(queueUrl).withMessageBody("body");
        sqsClient.sendMessage(request);
    }

    @Trace(dispatcher = true)
    private void sendMessageBatch() {
        SendMessageBatchRequest request = (new SendMessageBatchRequest()).withQueueUrl(queueUrl);
        sqsClient.sendMessageBatch(request);
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
