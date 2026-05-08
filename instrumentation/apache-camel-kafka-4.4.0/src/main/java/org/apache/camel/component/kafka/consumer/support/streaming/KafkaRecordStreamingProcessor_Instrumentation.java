package org.apache.camel.component.kafka.consumer.support.streaming;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import com.nr.camel.kafka440.CamelKafkaUtil;
import org.apache.camel.component.kafka.KafkaConsumer;
import org.apache.camel.component.kafka.consumer.support.ProcessingResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

@Weave(originalName="org.apache.camel.component.kafka.consumer.support.streaming.KafkaRecordStreamingProcessor", type= MatchType.ExactClass)
class KafkaRecordStreamingProcessor_Instrumentation {

    @Trace(dispatcher=true)
    public ProcessingResult processExchange(
            KafkaConsumer camelKafkaConsumer, TopicPartition topicPartition, boolean partitionHasNext,
            boolean recordHasNext, ConsumerRecord<Object, Object> consumerRecord) {
        CamelKafkaUtil.reportExternal(consumerRecord);
        return Weaver.callOriginal();
    }

}
