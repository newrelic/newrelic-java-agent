package com.nr.camel.kafka3214;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.TransportType;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class CamelKafkaUtil {

    public static void reportExternal(ConsumerRecord<?, ?> record) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            txn.acceptDistributedTraceHeaders(TransportType.Kafka, new HeadersWrapper(record.headers()));
        }
    }
}
