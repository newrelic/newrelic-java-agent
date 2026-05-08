package org.apache.camel.component.kafka.consumer.support;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.camel.kafka315.CamelKafkaUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

@Weave(originalName = "org.apache.camel.component.kafka.consumer.support.KafkaRecordProcessorFacade", type = MatchType.ExactClass)
public class KafkaRecordProcessorFacade_Instrumentation {

    @Trace(dispatcher=true)
    private ProcessingResult processRecord(
            TopicPartition partition,
            boolean partitionHasNext,
            boolean recordHasNext,
            final ProcessingResult lastResult,
            KafkaRecordProcessor kafkaRecordProcessor,
            ConsumerRecord<Object, Object> consumerRecord) {
        ProcessingResult processingResult = Weaver.callOriginal();
        CamelKafkaUtil.reportExternal(consumerRecord);
        return processingResult;
    }
}
