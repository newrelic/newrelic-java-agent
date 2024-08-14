/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.kafka;

import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.testcontainers.containers.KafkaContainer;

public class KafkaHelper {

    public static KafkaProducer<String, String> newProducer(KafkaContainer kafkaContainer) {
        Properties props = getProps(kafkaContainer.getBootstrapServers());
        return new KafkaProducer<>(props);
    }

    public static KafkaConsumer<String, String> newConsumer(KafkaContainer kafkaContainer) {
        Properties props = getProps(kafkaContainer.getBootstrapServers());
        return new KafkaConsumer<>(props);
    }

    public static Properties getProps(String bootstrapServers) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", "test-consumer-group");
        props.put("group.protocol", "CLASSIC");
        return props;
    }

    private KafkaHelper() {
        // prevents instantiations
    }
}
