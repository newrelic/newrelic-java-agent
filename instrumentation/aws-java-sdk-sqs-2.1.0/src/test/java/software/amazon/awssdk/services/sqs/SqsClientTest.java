/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sqs;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.sqs" }, configName = "dt_enabled.yml")
public class SqsClientTest {

    private SqsClient sqsClient;
    private SQSRestServer server;
    private String queueUrl;
    private final String QUEUE_NAME = "myQueue";

    @Before
    public void before() throws Exception {
        int port = InstrumentationTestRunner.getIntrospector().getRandomPort();
        server = SQSRestServerBuilder.withPort(port).withInterface("localhost").start();

        String endpoint = "http://localhost:" + port;
        sqsClient = SqsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                .endpointOverride(new URI(endpoint))
                .region(Region.AP_NORTHEAST_1)
                .build();

        CreateQueueResponse myQueue = sqsClient.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build());
        queueUrl = myQueue.queueUrl();
    }

    @After
    public void after() {
        server.stopAndWait();
    }

    @Test
    public void testSendMessage() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        sendMessageRequest();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        String metricName = "MessageBroker/SQS/Queue/Produce/Named/" + QUEUE_NAME;
        checkScopedMetricCount(txName, metricName, 1);
        checkSpanAttrs(metricName);
    }

    @Test
    public void testSendMessageBatch() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        sendMessageBatch();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        String metricName = "MessageBroker/SQS/Queue/Produce/Named/" + QUEUE_NAME;
        checkScopedMetricCount(txName, metricName, 1);
        checkSpanAttrs(metricName);
    }

    @Test
    public void testReceiveMessage() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        receiveMessage();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        String metricName = "MessageBroker/SQS/Queue/Consume/Named/" + QUEUE_NAME;
        checkScopedMetricCount(txName, metricName, 1);
        checkSpanAttrs(metricName);
    }

    @Trace(dispatcher = true)
    private void sendMessageRequest() {
        SendMessageRequest request = SendMessageRequest.builder().queueUrl(queueUrl).messageBody("body").build();
        sqsClient.sendMessage(request);
    }

    @Trace(dispatcher = true)
    private void sendMessageBatch() {
        SendMessageBatchRequest request = SendMessageBatchRequest.builder().queueUrl(queueUrl).build();
        sqsClient.sendMessageBatch(request);
    }

    @Trace(dispatcher = true)
    private void receiveMessage() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder().queueUrl(queueUrl).build();
        sqsClient.receiveMessage(request);
    }

    public static void checkScopedMetricCount(String transactionName, String metricName, int expected) {
        TracedMetricData data = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(
                transactionName).get(metricName);
        Assert.assertEquals(expected, data.getCallCount());
    }

    public void checkSpanAttrs(String metricName) {
        SpanEvent messageSpan = InstrumentationTestRunner.getIntrospector().getSpanEvents().stream()
                .filter(span -> span.getName().equals(metricName))
                .findFirst().orElseThrow(() -> new RuntimeException("Could not find span"));
        Map<String, Object> agentAttrs = messageSpan.getAgentAttributes();
        assertEquals("aws_sqs", agentAttrs.get("messaging.system"));
        assertEquals(QUEUE_NAME, agentAttrs.get("messaging.destination.name"));
    }
}
