package com.nr.camel.kafka440;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public class CamelKafkaUtil {
    public static final boolean DT_CONSUMER_BATCH_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consume_many.enabled", false);
    public static String LIBRARY_NAME = "ApacheCamelKafka";

    public static void reportExternal(ConsumerRecord<?, ?> record) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            txn.acceptDistributedTraceHeaders(TransportType.Kafka, new HeadersWrapper(record.headers()));
        }
        NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageConsumeParameters.library(LIBRARY_NAME)
                .destinationType(DestinationType.NAMED_TOPIC)
                .destinationName(record.topic())
                .inboundHeaders(null)
                .build());
    }

    public static void reportBatchExternal(ConsumerRecords<?, ?> records) {
        if (DT_CONSUMER_BATCH_ENABLED && records.count() > 0) {
            ConsumerRecord<?, ?> firstRecord = records.iterator().next();
            reportExternal(firstRecord);
        }
    }
}
