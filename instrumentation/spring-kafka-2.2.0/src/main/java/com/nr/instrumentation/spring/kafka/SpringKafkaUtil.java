package com.nr.instrumentation.spring.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpringKafkaUtil {

    public static Map<Object, Boolean> handlerCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    public static Map<Object, Boolean> listenerCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static String CATEGORY = "Message";
    public static String LIBRARY = "Kafka";

    private static final boolean DT_CONSUMER_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consume_many.enabled", false);

    public static <T> void processMessageListener(T data) {
        if (listenerCache.containsKey(data)) {
            return;
        }

        if (data instanceof ConsumerRecord) {
            ConsumerRecord<?, ?> record = ((ConsumerRecord<?, ?>) data);
            List<ConsumerRecord<?, ?>> records = Collections.singletonList(record);
            reportExternal(records, false);
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                    false, CATEGORY, "Kafka/Listen/Topic/Named", record.topic());
        } else if (data instanceof ConsumerRecords) {
            reportExternal((ConsumerRecords<?, ?>) data, true);
        } else if (data instanceof List) {
            reportExternal((List<?>) data, true);
        }

        listenerCache.put(data, true);
    }

    // Creates external spans and sets a transaction name based on the method annotation.
    // This is given lower priority over naming a transaction based on a single message listener.
    public static void nameHandlerTransaction(Message<?> message, InvocableHandlerMethod handlerMethod) {
        if (AgentBridge.getAgent().getTransaction(false) == null || handlerCache.containsKey(message)) {
            return;
        }
        String transactionName = "Kafka/Listen";
        if (handlerMethod != null) {
            KafkaListener annotation = handlerMethod.getMethod().getAnnotation(KafkaListener.class);
            if (annotation != null) {
                transactionName = "Kafka/Listen/Topic/Named/" + String.join(",", annotation.topics());
            }
        }
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, CATEGORY, transactionName);
        handlerCache.put(message, Boolean.TRUE);
    }

    private static void reportExternal(Iterable<?> consumerRecords, boolean isBatch) {
        boolean canAddHeaders = !isBatch || DT_CONSUMER_ENABLED;
        for (Object object: consumerRecords) {
            ConsumerRecord<?, ?> record = (object instanceof ConsumerRecord) ? (ConsumerRecord<?, ?>) object : null;
            if (record == null) {
                break;
            }
            HeadersWrapper headers = canAddHeaders ? new HeadersWrapper(record.headers()) : null;
            MessageConsumeParameters params = MessageConsumeParameters.library(LIBRARY)
                    .destinationType(DestinationType.NAMED_TOPIC)
                    .destinationName(record.topic())
                    .inboundHeaders(headers)
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
            break;
        }
    }

}
