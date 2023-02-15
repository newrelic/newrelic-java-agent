package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

@Weave(type = MatchType.Interface, originalName = "org.apache.kafka.streams.processor.internals.Task")
public abstract class Task_Instrumentation {

    @Trace
    public void addRecords(TopicPartition partition, Iterable<ConsumerRecord<byte[], byte[]>> records) {
        NewRelic.getAgent().getTransaction().getTracedMethod()
                .setMetricName("Java/KafkaStreams/Topic/Named/" + partition.topic() + "/Task/addRecords");
        Weaver.callOriginal();
    }
}
