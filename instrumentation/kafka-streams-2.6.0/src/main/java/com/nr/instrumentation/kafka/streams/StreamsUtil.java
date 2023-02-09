package com.nr.instrumentation.kafka.streams;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamsUtil {
    private StreamsUtil() {}

    public static void initTransaction() {
        LoopState.LOCAL.set(new LoopState());
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "KafkaStreams",
                "MessageBroker/Kafka/Streams/EventLoop/Iteration");
    }

    // Records number of records poll to loop state
    public static void handlePolledRecords(ConsumerRecords<byte[], byte[]> records) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            LoopState state = LoopState.LOCAL.get();
            if (state != null) {
                int polled = records == null ? 0 : records.count();
                state.incRecordsPolled(polled);
            }
            Iterator<ConsumerRecord<byte[], byte[]>> recordsIterator = records.iterator();
            // Topic that is already reported by kafka-clients instrumentation module
            String topicToExclude = recordsIterator.hasNext() ? recordsIterator.next().topic() : null;
            Set<String> topicsToReport = records.partitions().stream()
                    .map(TopicPartition::topic)
                    .distinct()
                    .filter(t -> !Objects.equals(t, topicToExclude)) // Exclude this topic to prevent it from being reported twice
                    .collect(Collectors.toSet());
            for (String topic : topicsToReport) {
                MessageConsumeParameters params = MessageConsumeParameters.library("Kafka")
                        .destinationType(DestinationType.NAMED_TOPIC)
                        .destinationName(topic)
                        .inboundHeaders(null)
                        .build();
                NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(params);
            }
        }
    }

    public static void handleOutgoingRecord(final ProducerRecord<byte[], byte[]> record) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            MessageProduceParameters params = MessageProduceParameters.library("Kafka")
                    .destinationType(DestinationType.NAMED_TOPIC)
                    .destinationName(record.topic())
                    .outboundHeaders(null)
                    .build();
            NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(params);
        }
    }

    public static void updateTotalProcessed(double processed) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            LoopState state = LoopState.LOCAL.get();
            if (state != null) {
                state.incTotalProcessed(processed);
            }
        }
    }

    public static void finalizeLoopState() {
        LoopState state = LoopState.LOCAL.get();
        if (state != null && state.getRecordsPolled() == 0 && state.getTotalProcessed() == 0) {
            NewRelic.getAgent().getTransaction().ignore();
        }
        LoopState.LOCAL.remove();
    }
}
