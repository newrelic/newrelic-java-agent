/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.kafka;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import static com.newrelic.agent.introspec.MetricsHelper.getUnscopedMetricCount;

import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

//@Ignore("This test is flaky on GHA")
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka")
public class Kafka37MessageTest {
    @Rule
    public KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    private final String TOPIC = "life-universe-everything";
    private final String ANOTHER_TOPIC = "vogon-poetry";
    @Before
    public void before() {
        kafkaContainer.start();
    }

    @After
    public void after() {
        kafkaContainer.stop();
    }

    @Test
    public void testProducer() throws ExecutionException, InterruptedException {
        Future<Boolean> msgsWereRead = asyncReadMessages();

        // giving some time for the consumer to ready itself up prior to sending the messages
        Thread.sleep(1000L);
        sendMessages();

        Assert.assertTrue("Messages weren't read", msgsWereRead.get());
        assertUnscopedMetrics();
    }

    /**
     * @return a Future that holds whether the messages were read
     */
    private Future<Boolean> asyncReadMessages() {
        return Executors.newSingleThreadExecutor().submit(this::readMessages);
    }

    /**
     * @return whether messages were read
     */
    @Trace(dispatcher = true)
    private boolean readMessages() throws InterruptedException {
        int messagesRead = 0;
        try (KafkaConsumer<String, String> consumer = KafkaHelper.newConsumer(kafkaContainer)) {
            consumer.subscribe(Collections.singleton(TOPIC));

            // setting a timeout so this does not drag forever if something goes wrong.
            long waitUntil = System.currentTimeMillis() + 15000L;
            while (waitUntil > System.currentTimeMillis()) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                messagesRead += records.count();
                if (messagesRead == 2) {
                    // Sleep for a minute before closing the consumer so MetricsScheduler runs
                    // few times and all metrics are reported
                    Thread.sleep(60000L);
                    return true;
                }
            }
        }
        return false;
    }

    @Trace(dispatcher = true)
    private void sendMessages() throws ExecutionException, InterruptedException {
        try (KafkaProducer<String, String> producer = KafkaHelper.newProducer(kafkaContainer)) {
            List<Future<RecordMetadata>> futures = Arrays.asList(
                    producer.send(new ProducerRecord<>(ANOTHER_TOPIC, "Oh freddled gruntbuggly")),
                    producer.send(new ProducerRecord<>(TOPIC, "Life, don't talk to me about life.")),
                    producer.send(new ProducerRecord<>(TOPIC, "Don't Panic"))
            );
            for (Future<RecordMetadata> future : futures) {
                future.get();
            }
            // Sleep for a minute before closing the producer so MetricsScheduler runs
            // few times and all metrics are reported
            Thread.sleep(60000L);
        }
    }

    private void assertUnscopedMetrics() {
        // on the previous instrumentation module there are more metrics being verified.
        // Kafka 3 changed a little how the values are retrieved and those metrics now return NaN, and thus are not reported.
        assertUnscopedMetricExists(
                // general kafka metrics, they can change from test to test, so only verifying they exist
                "MessageBroker/Kafka/Internal/consumer-coordinator-metrics/assigned-partitions",
                "MessageBroker/Kafka/Internal/consumer-coordinator-metrics/commit-rate",
                "MessageBroker/Kafka/Internal/consumer-coordinator-metrics/heartbeat-rate",
                "MessageBroker/Kafka/Internal/consumer-coordinator-metrics/join-rate",
                "MessageBroker/Kafka/Internal/consumer-coordinator-metrics/last-heartbeat-seconds-ago",
                "MessageBroker/Kafka/Internal/consumer-coordinator-metrics/sync-rate",
                "MessageBroker/Kafka/Internal/consumer-fetch-manager-metrics/bytes-consumed-rate",
                "MessageBroker/Kafka/Internal/consumer-fetch-manager-metrics/fetch-rate",
                "MessageBroker/Kafka/Internal/consumer-fetch-manager-metrics/records-consumed-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/connection-close-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/connection-count",
                "MessageBroker/Kafka/Internal/consumer-metrics/connection-creation-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/incoming-byte-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/io-ratio",
                "MessageBroker/Kafka/Internal/consumer-metrics/io-wait-ratio",
                "MessageBroker/Kafka/Internal/consumer-metrics/network-io-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/outgoing-byte-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/request-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/response-rate",
                "MessageBroker/Kafka/Internal/consumer-metrics/select-rate",
                "MessageBroker/Kafka/Internal/kafka-metrics-count/count",
                "MessageBroker/Kafka/Internal/producer-metrics/batch-split-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/buffer-available-bytes",
                "MessageBroker/Kafka/Internal/producer-metrics/buffer-exhausted-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/buffer-total-bytes",
                "MessageBroker/Kafka/Internal/producer-metrics/bufferpool-wait-ratio",
                "MessageBroker/Kafka/Internal/producer-metrics/connection-close-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/connection-count",
                "MessageBroker/Kafka/Internal/producer-metrics/connection-creation-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/incoming-byte-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/io-ratio",
                "MessageBroker/Kafka/Internal/producer-metrics/io-wait-ratio",
                "MessageBroker/Kafka/Internal/producer-metrics/metadata-age",
                "MessageBroker/Kafka/Internal/producer-metrics/network-io-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/outgoing-byte-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/record-error-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/record-retry-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/record-send-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/request-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/requests-in-flight",
                "MessageBroker/Kafka/Internal/producer-metrics/response-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/select-rate",
                "MessageBroker/Kafka/Internal/producer-metrics/waiting-threads"
        );

        // serializer are called more often because they serialize the key and the value
        assertEquals(0, getUnscopedMetricCount("MessageBroker/Kafka/Deserialization/" + TOPIC));
        assertEquals(4, getUnscopedMetricCount("MessageBroker/Kafka/Serialization/" + TOPIC));
        assertEquals(2, getUnscopedMetricCount("MessageBroker/Kafka/Topic/Produce/Named/" +TOPIC));

        // deserializer is never called because this topic is never read from
        assertEquals(0, getUnscopedMetricCount("MessageBroker/Kafka/Deserialization/" + ANOTHER_TOPIC));
        assertEquals(2, getUnscopedMetricCount("MessageBroker/Kafka/Serialization/" + ANOTHER_TOPIC));
        assertEquals(1, getUnscopedMetricCount("MessageBroker/Kafka/Topic/Produce/Named/" + ANOTHER_TOPIC));

        // there are 2 messages in the topic, but they could be read in a single poll, or in 2
        int consumedCount = getUnscopedMetricCount("MessageBroker/Kafka/Topic/Consume/Named/" + TOPIC);
        assertTrue(consumedCount >= 1);
        assertTrue(consumedCount <= 2);

        assertEquals(0, getUnscopedMetricCount("MessageBroker/Kafka/Rebalance/Assigned/life-universe-everything/0"));

        // Nodes metrics
        assertTrue(unscopedNodesMetricExists("MessageBroker/Kafka/Nodes/localhost:[0-9]*"));
        assertTrue(unscopedNodesMetricExists("MessageBroker/Kafka/Nodes/localhost:[0-9]*/Consume/" + TOPIC));
        assertTrue(unscopedNodesMetricExists("MessageBroker/Kafka/Nodes/localhost:[0-9]*/Produce/" + TOPIC));
        assertFalse(unscopedNodesMetricExists("MessageBroker/Kafka/Nodes/localhost:[0-9]*/Consume/" + ANOTHER_TOPIC));
        assertTrue(unscopedNodesMetricExists("MessageBroker/Kafka/Nodes/localhost:[0-9]*/Produce/" + ANOTHER_TOPIC));
    }

    private void assertUnscopedMetricExists(String ... metricNames) {
        int notFoundMetricCount = 0;
        Set<String> existingMetrics= InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().keySet();
        for (String metricName : metricNames) {
            Assert.assertTrue("metric not found: " + metricName, existingMetrics.contains(metricName));
        }
        System.out.println(notFoundMetricCount + " metrics not found");
    }

    private boolean unscopedNodesMetricExists(String metricName) {
        return InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().keySet().stream()
                .anyMatch(key -> key.matches(metricName));
    }
}
