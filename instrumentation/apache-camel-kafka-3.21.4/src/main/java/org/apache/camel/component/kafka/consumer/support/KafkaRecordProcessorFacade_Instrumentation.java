package org.apache.camel.component.kafka.consumer.support;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.camel.kafka3214.CamelKafkaUtil;
import org.apache.camel.Exchange;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

@Weave(originalName = "org.apache.camel.component.kafka.consumer.support.KafkaRecordProcessorFacade", type = MatchType.ExactClass)
public class KafkaRecordProcessorFacade_Instrumentation {

    @Trace(dispatcher=true)
    private ProcessingResult processRecord(TopicPartition partition, boolean partitionHasNext, boolean recordHasNext,
            KafkaRecordProcessor kafkaRecordProcessor, ConsumerRecord<Object, Object> record) {
        ProcessingResult processingResult = Weaver.callOriginal();
        CamelKafkaUtil.reportExternal(record);
        return processingResult;
    }
}
