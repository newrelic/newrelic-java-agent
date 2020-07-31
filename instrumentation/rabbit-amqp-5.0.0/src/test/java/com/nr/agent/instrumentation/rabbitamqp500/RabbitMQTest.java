/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.rabbitamqp500;

import com.newrelic.agent.introspec.*;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.rabbitmq.client.*;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.PredefinedVersion;
import io.arivera.oss.embedded.rabbitmq.apache.commons.lang3.SystemUtils;
import io.arivera.oss.embedded.rabbitmq.util.RandomPortSupplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.rabbitmq.client", "com.rabbitmq.client.impl" }, configName = "distributed_tracing.yml")
public class RabbitMQTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private static final String DEFAULT_EXCHANGE = "";

    private static EmbeddedRabbitMq rabbitMq;

    private Channel channel;

    private static int port;

    @BeforeClass
    public static void beforeClass() throws IOException {
        port = new RandomPortSupplier().get();
        // Server
        EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
                .version(PredefinedVersion.V3_6_9)
                .downloadFolder(folder.newFolder("download"))
                .extractionFolder(folder.newFolder("extraction"))
                .rabbitMqServerInitializationTimeoutInMillis(60 * 1000)
                .defaultRabbitMqCtlTimeoutInMillis(60 * 1000)
                .envVar("RABBITMQ_NODENAME", "RabbitMQ" + port)
                .erlangCheckTimeoutInMillis(5000)
                .port(port)
                .build();

        rabbitMq = new EmbeddedRabbitMq(config);
        rabbitMq.start();
    }

    @AfterClass
    public static void afterClass() {
        rabbitMq.stop();
    }

    @Before
    public void setUp() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setPort(port);
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
        InstrumentationTestRunner.getIntrospector().clearSpanEvents();
    }

    @After
    public void tearDown() throws IOException {
        channel.getConnection().close();
    }

    @Test
    public void testProduceConsumePurge() throws IOException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        Map<String, Object> headers = new HashMap<>();
        headers.put("keyOne", 1);
        headers.put("keyTwo", 2);

        String queueOne = UUID.randomUUID().toString();
        putGetAndPurge(DEFAULT_EXCHANGE, "direct", queueOne, null, null, null);
        String queueOneTxn = MessageFormat.format("OtherTransaction/Test/{0}", queueOne);
        assertTrue(introspector.getTransactionNames().contains(queueOneTxn));
        assertProduceConsumePurgeMetrics("Default", queueOne, introspector.getMetricsForTransaction(queueOneTxn));
        assertProduceConsumeTraceAttrs(introspector.getTransactionTracesForTransaction(queueOneTxn).iterator().next(),
                "Default", null, null, Collections.emptyMap());

        String queueTwo = UUID.randomUUID().toString();
        putGetAndPurge("MyExchange", "direct", queueTwo, "replyTo", "correlation-id", headers);
        String queueTwoTxn = MessageFormat.format("OtherTransaction/Test/{0}", queueTwo);
        assertTrue(introspector.getTransactionNames().contains(queueTwoTxn));
        assertProduceConsumePurgeMetrics("MyExchange", queueTwo, introspector.getMetricsForTransaction(queueTwoTxn));
        assertProduceConsumeTraceAttrs(introspector.getTransactionTracesForTransaction(queueTwoTxn).iterator().next(),
                "MyExchange", "replyTo", "correlation-id", Collections.emptyMap());

        String queueThree = UUID.randomUUID().toString();
        putGetAndPurge("direct", "direct", queueThree, null, null, null);
        String queueThreeTxn = MessageFormat.format("OtherTransaction/Test/{0}", queueThree);
        assertTrue(introspector.getTransactionNames().contains(queueThreeTxn));
        assertProduceConsumePurgeMetrics("direct", queueThree, introspector.getMetricsForTransaction(queueThreeTxn));
        assertProduceConsumeTraceAttrs(introspector.getTransactionTracesForTransaction(queueThreeTxn).iterator().next(),
                "direct", null, null, Collections.emptyMap());

        String queueFour = UUID.randomUUID().toString();
        putGetAndPurge("TopicExchange", "topic", queueFour, "replyTo", null, headers);
        String queueFourTxn = MessageFormat.format("OtherTransaction/Test/{0}", queueFour);
        assertTrue(introspector.getTransactionNames().contains(queueFourTxn));
        assertProduceConsumePurgeMetrics("TopicExchange", queueFour,
                introspector.getMetricsForTransaction(queueFourTxn));
        assertProduceConsumeTraceAttrs(introspector.getTransactionTracesForTransaction(queueFourTxn).iterator().next(),
                "TopicExchange", "replyTo", null, Collections.emptyMap());

        String queueFive = UUID.randomUUID().toString();
        putGetAndPurge("headers", "headers", queueFive, null, "correlation-id", headers);
        String queueFiveTxn = MessageFormat.format("OtherTransaction/Test/{0}", queueFive);
        assertTrue(introspector.getTransactionNames().contains(queueFiveTxn));
        assertProduceConsumePurgeMetrics("headers", queueFive, introspector.getMetricsForTransaction(queueFiveTxn));
        assertProduceConsumeTraceAttrs(introspector.getTransactionTracesForTransaction(queueFiveTxn).iterator().next(),
                "headers", null, "correlation-id", Collections.emptyMap());
    }

    @Test
    public void testMessageListener() throws IOException, InterruptedException {
        final String queueName = UUID.randomUUID().toString();
        final String messageForListener = "Hello message listener!";

        channel.queueDeclare(queueName, false, false, true, Collections.emptyMap());
        channel.basicPublish(DEFAULT_EXCHANGE, queueName, new AMQP.BasicProperties(), messageForListener.getBytes());
        channel.basicConsume(queueName, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) {
                assertEquals(messageForListener, new String(body));
            }
        });

        // Let handleDelivery Transaction to finish.
        Thread.sleep(1000);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String expectedTransactionName = "OtherTransaction/Message/RabbitMQ/Exchange/Named/Default";
        Collection<String> transactionNames = introspector.getTransactionNames();
        assertTrue(transactionNames.contains(expectedTransactionName));

        //Do not record consume metric, message has already been delivered
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertFalse(metrics.containsKey("MessageBroker/RabbitMQ/Exchange/Consume/Named/Default"));
    }

    @Trace(dispatcher = true)
    public void putGetAndPurge(String exchangeName, String exchangeType, String queueName, String replyTo,
            String correlationId, Map<String, Object> headers)
            throws IOException {
        channel.queueDeclare(queueName, false, false, true, Collections.emptyMap());

        if (!exchangeName.equals(DEFAULT_EXCHANGE)) {
            channel.exchangeDeclare(exchangeName, exchangeType);
            channel.queueBind(queueName, exchangeName, queueName);
        }

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.replyTo(replyTo);
        builder.correlationId(correlationId);
        builder.headers(headers);
        channel.basicPublish(exchangeName, queueName, builder.build(), "message".getBytes());

        GetResponse response = channel.basicGet(queueName, true);
        assertEquals("message", new String(response.getBody()));

        NewRelic.getAgent()
                .getTransaction()
                .setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Test", queueName);

        channel.queuePurge(queueName);
    }

    private void assertProduceConsumePurgeMetrics(String exchangeName, String queueName,
            Map<String, TracedMetricData> metrics) {
        String consumeMetric = "MessageBroker/RabbitMQ/Exchange/Consume/Named/" + exchangeName;
        assertTrue(metrics.containsKey(consumeMetric));
        assertEquals(1, metrics.get(consumeMetric).getCallCount());

        String produceMetric = "MessageBroker/RabbitMQ/Exchange/Produce/Named/" + exchangeName;
        assertTrue(metrics.containsKey(produceMetric));
        assertEquals(1, metrics.get(produceMetric).getCallCount());

        String purgeMetric = "MessageBroker/RabbitMQ/Queue/Purge/Named/" + queueName;
        assertTrue(metrics.containsKey(purgeMetric));
        assertEquals(1, metrics.get(purgeMetric).getCallCount());
    }

    private void assertProduceConsumeTraceAttrs(TransactionTrace trace, String exchangeName, String replyTo,
            String correlationId, Map<String, Object> headers) {
        // Collect all segments
        Map<String, TraceSegment> segments = new HashMap<>();
        Queue<TraceSegment> queue = new LinkedList<>();
        queue.offer(trace.getInitialTraceSegment());
        while (!queue.isEmpty()) {
            TraceSegment segment = queue.poll();
            segments.put(segment.getName(), segment);
            queue.addAll(segment.getChildren());
        }

        TraceSegment produceSegment = segments.get("MessageBroker/RabbitMQ/Exchange/Consume/Named/" + exchangeName);
        assertTrue(produceSegment.getTracerAttributes().containsKey("message.routingKey"));
        assertEquals(replyTo, produceSegment.getTracerAttributes().get("message.replyTo"));
        assertEquals(correlationId, produceSegment.getTracerAttributes().get("message.correlationId"));

        for (String key : headers.keySet()) {
            assertNotNull(produceSegment.getTracerAttributes().get("message." + key));
        }

        TraceSegment consumeSegment = segments.get("MessageBroker/RabbitMQ/Exchange/Consume/Named/" + exchangeName);
        assertTrue(consumeSegment.getTracerAttributes().containsKey("message.routingKey"));
        assertTrue(consumeSegment.getTracerAttributes().containsKey("message.queueName"));
        assertEquals(replyTo, consumeSegment.getTracerAttributes().get("message.replyTo"));

        for (String key : headers.keySet()) {
            assertNotNull(consumeSegment.getTracerAttributes().get("message." + key));
        }
    }

    @Test
    public void testBasicDistributedTracingAndSpans() throws IOException, InterruptedException {
        final String queueName = UUID.randomUUID().toString();
        final String messageForListener = "Hello message listener!";

        channel.queueDeclare(queueName, false, false, true, Collections.emptyMap());
        channel.basicPublish(DEFAULT_EXCHANGE, queueName, new AMQP.BasicProperties(), messageForListener.getBytes());
        channel.basicConsume(queueName, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) {
                assertEquals(messageForListener, new String(body));
            }
        });

        // Let handleDelivery Transaction to finish.
        Thread.sleep(1000);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String expectedTransactionName = "OtherTransaction/Message/RabbitMQ/Exchange/Named/Default";
        Collection<TransactionEvent> events = introspector.getTransactionEvents(expectedTransactionName);
        TransactionEvent event = (TransactionEvent) events.toArray()[0];
        assertTrue(event.sampled());

        //SpanAssertions
        String expectedSpanName = "Java/com.nr.agent.instrumentation.rabbitamqp500.RabbitMQTest$2/handleDelivery";
        Collection<SpanEvent> externalSpanEvents = SpanEventsHelper.getSpanEventsByCategory(SpanCategory.generic);
        assertEquals(1, externalSpanEvents.size());
        SpanEvent externalSpanEvent = externalSpanEvents.iterator().next();

        //The guid of the transaction is the same as the TraceId
        assertEquals(externalSpanEvent.getTransactionId(), event.getMyGuid());
        assertNotNull(externalSpanEvent.traceId());
        assertEquals(externalSpanEvent.getName(), expectedSpanName);
    }

    @Test
    public void testCrossApplicationDistributedTracing() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);

        final String queueName = UUID.randomUUID().toString();
        final String replyMessage = "reply";
        final String exchangeName = "MyFavoriteExchange";

        channel.exchangeDeclare(exchangeName, "topic");
        channel.queueDeclare(queueName, false, false, true, Collections.emptyMap());
        channel.queueBind(queueName, exchangeName, queueName);

        channel.basicConsume(queueName, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                channel.basicPublish(DEFAULT_EXCHANGE, properties.getReplyTo(), new AMQP.BasicProperties(),
                        replyMessage.getBytes());
                latch.countDown();
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            @Trace(dispatcher = true)
            public void run() {
                NewRelic.setTransactionName("Category", "Sender");
                try {
                    String tempQueue = channel.queueDeclare().getQueue();
                    AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
                    builder.replyTo(tempQueue);
                    channel.basicPublish(exchangeName, queueName, builder.build(), "message".getBytes());

                    channel.basicConsume(tempQueue, true, ((consumerTag, message) -> {
                        assertEquals(replyMessage, new String(message.getBody()));
                        latch.countDown();
                    }), (consumerTag -> System.out.println("Consumer was cancelled")));

                } catch (IOException ignored) {
                }
            }
        });

        thread.start();
        thread.join(3000);
        latch.await();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String senderTransactioName = "OtherTransaction/Category/Sender";
        String messageListenerTransactionName = "OtherTransaction/Message/RabbitMQ/Exchange/Named/MyFavoriteExchange";
        String defaultExchangeTransactionName = "OtherTransaction/Message/RabbitMQ/Exchange/Named/Default";

        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(3, finishedTransactionCount);

        Collection<String> transactionNames = introspector.getTransactionNames();
        assertTrue(transactionNames.contains(senderTransactioName));
        assertTrue(transactionNames.contains(messageListenerTransactionName));
        assertTrue(transactionNames.contains(defaultExchangeTransactionName));

        String parentAppId = "1xyz3333";
        String parentAccountId = "1xyz234";
        String parentTransportType = "AMQP";
        //Distributed tracing asserts
        TransactionEvent senderEvent = (TransactionEvent) introspector.getTransactionEvents(senderTransactioName).toArray()[0];
        TransactionEvent messageListenerEvent = (TransactionEvent)
                introspector.getTransactionEvents(messageListenerTransactionName).toArray()[0];
        TransactionEvent defaultExchangeEvent = (TransactionEvent)
                introspector.getTransactionEvents(defaultExchangeTransactionName).toArray()[0];

        assertEquals(messageListenerEvent.getParentApplicationId(), parentAppId);
        assertEquals(defaultExchangeEvent.getParentApplicationId(), parentAppId);

        assertEquals(messageListenerEvent.getParentAccountId(), parentAccountId);
        assertEquals(defaultExchangeEvent.getParentAccountId(), parentAccountId);

        assertEquals(messageListenerEvent.getParentTransportType(), parentTransportType);
        assertEquals(defaultExchangeEvent.getParentTransportType(), parentTransportType);

        assertEquals(messageListenerEvent.getParentId(), senderEvent.getMyGuid());
        assertEquals(defaultExchangeEvent.getParentId(), messageListenerEvent.getMyGuid());

        assertEquals(messageListenerEvent.getParentSpanId(), messageListenerEvent.getReferrerGuid());
        assertEquals(defaultExchangeEvent.getParentSpanId(), defaultExchangeEvent.getReferrerGuid());

        Collection<SpanEvent> externalSpanEvents = SpanEventsHelper.getSpanEventsByCategory(SpanCategory.generic);
        assertEquals(5, externalSpanEvents.size());
    }
}
