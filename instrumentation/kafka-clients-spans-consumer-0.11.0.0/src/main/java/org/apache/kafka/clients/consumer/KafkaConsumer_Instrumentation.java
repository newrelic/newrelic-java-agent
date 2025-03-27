package org.apache.kafka.clients.consumer;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.HeadersWrapper;

import java.time.Duration;
import java.util.logging.Level;

@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public class KafkaConsumer_Instrumentation<K, V> {

    public ConsumerRecords<K, V> poll(final Duration timeout) {
        final ConsumerRecords<K, V> records = Weaver.callOriginal();
        nrAcceptDtHeaders(records);
        return records;
    }

    public ConsumerRecords<K, V> poll(final long timeoutMs) {
        final ConsumerRecords<K, V> records = Weaver.callOriginal();
        nrAcceptDtHeaders(records);
        return records;
    }

    private void nrAcceptDtHeaders(ConsumerRecords<K, V> records) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            for (ConsumerRecord<?, ?> record : records) {
                Headers dtHeaders = new HeadersWrapper(record.headers());
                NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Kafka, dtHeaders);
                break;
            }
        }
    }
}
