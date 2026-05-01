package org.apache.kafka.clients.consumer;

import org.apache.kafka.clients.consumer.SubscriptionPattern;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public class KafkaConsumer_Instrumentation<K, V> {


    public void subscribe(SubscriptionPattern pattern) {
        Weaver.callOriginal();
    }

}
