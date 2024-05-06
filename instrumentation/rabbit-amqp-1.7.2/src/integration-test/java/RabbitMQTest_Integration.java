import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.rabbitmq.client", "com.rabbitmq.client.impl"})
public class RabbitMQTest_Integration {
    private Channel channel;
    private String DEFAULT_EXCHANGE = "";

    @Before
    public void setUp() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection("localhost");
        channel = connection.createChannel();
    }

    @After
    public void tearDown() throws IOException {
        channel.getConnection().close();
    }

    @Test
    public void testProduceConsume() throws IOException {
        final String queueName = UUID.randomUUID().toString();
        putAndGetInTransaction(queueName);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String expectedTransactionName = String.format("OtherTransaction/Message/RabbitMQ/Queue/Named/%s", queueName);
        final Collection<String> transactionNames = introspector.getTransactionNames();
        assertTrue(transactionNames.contains(expectedTransactionName));

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertTrue(metrics.containsKey(String.format("MessageBroker/RabbitMQ/Queue/Consume/Named/%s", queueName)));
        assertTrue(metrics.containsKey(String.format("MessageBroker/RabbitMQ/Queue/Produce/Named/%s", queueName)));
    }

    @Test
    public void testMessageListener() throws IOException, InterruptedException {
        final String queueName = UUID.randomUUID().toString();
        final String messageForListener = "Hello message listener!";

        channel.queueDeclare(queueName, false, false, true, true, Collections.<String, Object> emptyMap());
        channel.basicPublish(DEFAULT_EXCHANGE, queueName, new AMQP.BasicProperties(), messageForListener.getBytes());
        channel.basicConsume(queueName, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                assertEquals(messageForListener, new String(body));
            }
        });

        // Let handleDelivery Transaction to finish.
        Thread.sleep(1000);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String expectedTransactionName = String.format("OtherTransaction/Message/RabbitMQ/Queue/Named/%s", queueName);
        final Collection<String> transactionNames = introspector.getTransactionNames();
        assertTrue(transactionNames.contains(expectedTransactionName));

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertTrue(metrics.containsKey(String.format("MessageBroker/RabbitMQ/Queue/Consume/Named/%s", queueName)));
    }

    @Test
    public void testCat() throws IOException, InterruptedException {
        final String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, false, false, true, true, Collections.<String, Object> emptyMap());
        final String replyMessage = "reply";

        channel.basicConsume(queueName, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                channel.basicPublish(DEFAULT_EXCHANGE, properties.getReplyTo(), new AMQP.BasicProperties(),
                        replyMessage.getBytes());
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            @Trace(dispatcher = true)
            public void run() {
                NewRelic.setTransactionName("Category", "Sender");

                try {
                    String tempQueue = channel.queueDeclare().getQueue();
                    AMQP.BasicProperties properties = new AMQP.BasicProperties();
                    properties.setReplyTo(tempQueue);
                    channel.basicPublish(DEFAULT_EXCHANGE, queueName, properties, "message".getBytes());

                    QueueingConsumer queueingConsumer = new QueueingConsumer(channel);
                    channel.basicConsume(tempQueue, true, queueingConsumer);

                    // block
                    QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
                    assertEquals(replyMessage, new String(delivery.getBody()));

                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            }
        });

        thread.start();
        thread.join(2000);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String senderTransactioName = "OtherTransaction/Category/Sender";
        String messageListenerTransactionName = String.format("OtherTransaction/Message/RabbitMQ/Queue/Named/%s",
                queueName);

        final Collection<String> transactionNames = introspector.getTransactionNames();
        assertTrue(transactionNames.contains(senderTransactioName));
        assertTrue(transactionNames.contains(messageListenerTransactionName));

        Map<String, TracedMetricData> senderMetrics = introspector.getMetricsForTransaction(senderTransactioName);
        Map<String, TracedMetricData> messageListenerMetrics = introspector.getMetricsForTransaction(
                messageListenerTransactionName);

        assertTrue(senderMetrics.containsKey(String.format("MessageBroker/RabbitMQ/Queue/Produce/Named/%s",
                queueName)));
        assertTrue(senderMetrics.containsKey("MessageBroker/RabbitMQ/Queue/Consume/Temp"));

        assertTrue(messageListenerMetrics.containsKey(String.format("MessageBroker/RabbitMQ/Queue/Consume/Named/%s",
                queueName)));
        assertTrue(messageListenerMetrics.containsKey("MessageBroker/RabbitMQ/Queue/Produce/Temp"));

        // Ideally, the block below could be replaced with the following line:
        // CatHelper.verifyOneSuccessfulCat(introspector, senderTransactioName, messageListenerTransactionName);
        {
            TransactionTrace senderTT = introspector.getTransactionTracesForTransaction(
                    senderTransactioName).iterator().next();
            TransactionTrace messageListenerTT = introspector.getTransactionTracesForTransaction(
                    messageListenerTransactionName).iterator().next();

            Map<String, Object> senderTTIntrinsics = senderTT.getIntrinsicAttributes();
            Map<String, Object> messageListenerTTIntrinsics = messageListenerTT.getIntrinsicAttributes();

            assertNotNull(senderTTIntrinsics.get("trip_id"));
            assertNotNull(senderTTIntrinsics.get("path_hash"));
            assertNotNull(getAttribute(senderTT, "transaction_guid"));
            assertNotNull(messageListenerTTIntrinsics.get("referring_transaction_guid"));
            assertNotNull(messageListenerTTIntrinsics.get("client_cross_process_id"));

            TransactionEvent senderEvent = introspector.getTransactionEvents(senderTransactioName).iterator().next();
            TransactionEvent messageListenerEvent = introspector.getTransactionEvents(messageListenerTransactionName).iterator().next();

            assertEquals(senderEvent.getMyGuid(), messageListenerEvent.getReferrerGuid());
        }

    }

    private String getAttribute(TransactionTrace senderTT, String attributeName) {
        Queue<TraceSegment> queue = new LinkedList<TraceSegment>();
        queue.offer(senderTT.getInitialTraceSegment());

        while (!queue.isEmpty()) {
            TraceSegment segment = queue.poll();
            if (segment.getTracerAttributes().containsKey(attributeName)) {
                return (String) segment.getTracerAttributes().get(attributeName);
            }

            for (TraceSegment childSegment : segment.getChildren()) {
                queue.offer(childSegment);
            }
        }

        return null;
    }


    @Trace(dispatcher = true)
    public void putAndGetInTransaction(String queueName) throws IOException {
        channel.queueDeclare(queueName, false, false, true, true, Collections.<String, Object> emptyMap());

        AMQP.BasicProperties properties = new AMQP.BasicProperties();
        channel.basicPublish(DEFAULT_EXCHANGE, queueName, properties, "message".getBytes());

        GetResponse response = channel.basicGet(queueName, true);
        assertEquals("message", new String(response.getBody()));
    }

}