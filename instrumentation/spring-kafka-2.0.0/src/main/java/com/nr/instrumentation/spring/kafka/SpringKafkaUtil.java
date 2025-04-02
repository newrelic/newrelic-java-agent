package com.nr.instrumentation.spring.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Map;

public class SpringKafkaUtil {

    public static Map<Object, Boolean> recordsCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static String CATEGORY = "Message";
    public static String LIBRARY = "Kafka";

    private static final boolean DT_CONSUMER_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consumer_poll.enabled", false);

    public static <T> void processConsume(T record) {
        if (recordsCache.containsKey(record)) {
            return;
        }
        if (record instanceof ConsumerRecord) {
            processConsumeRecord((ConsumerRecord<?, ?>) record);
        } else if (record instanceof ConsumerRecords<?, ?>) {
            processConsumeBatch((ConsumerRecords<?, ?>) record);
        }
        recordsCache.put(record, true);
    }

    private static <K,V> void processConsumeRecord(ConsumerRecord<K, V> record) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                false, CATEGORY, "Kafka/Listen/Topic/Named", record.topic());

        HeadersWrapper inboundHeaders = new HeadersWrapper(record.headers());
        reportExternalConsume(record, inboundHeaders);
    }

    private static <K,V> void processConsumeBatch(ConsumerRecords<K,V> records) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                false, CATEGORY, "Kafka/Listen/Batch");

        for (ConsumerRecord<?, ?> record : records) {
            HeadersWrapper inboundHeaders = DT_CONSUMER_ENABLED ? new HeadersWrapper(record.headers()) : null;
            reportExternalConsume(record, inboundHeaders);
            break;
        }
    }

    private static <K,V> void reportExternalConsume(ConsumerRecord<K,V> record, HeadersWrapper inboundHeaders) {
        MessageConsumeParameters params = MessageConsumeParameters.library(LIBRARY)
                .destinationType(DestinationType.NAMED_TOPIC)
                .destinationName(record.topic())
                .inboundHeaders(inboundHeaders)
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
    }



}
