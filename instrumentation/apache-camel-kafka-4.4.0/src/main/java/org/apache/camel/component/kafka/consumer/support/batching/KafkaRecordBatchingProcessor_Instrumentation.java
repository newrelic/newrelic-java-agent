package org.apache.camel.component.kafka.consumer.support.batching;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.camel.kafka440.CamelKafkaUtil;
import org.apache.camel.component.kafka.KafkaConsumer;
import org.apache.camel.component.kafka.consumer.support.ProcessingResult;
import org.apache.kafka.clients.consumer.ConsumerRecords;

@Weave(originalName="org.apache.camel.component.kafka.consumer.support.batching.KafkaRecordBatchingProcessor", type= MatchType.ExactClass)
class KafkaRecordBatchingProcessor_Instrumentation {

    @Trace(dispatcher=true)
    public ProcessingResult processExchange(KafkaConsumer camelKafkaConsumer, ConsumerRecords<Object, Object> consumerRecords) {
        CamelKafkaUtil.reportBatchExternal(consumerRecords);
        return Weaver.callOriginal();
    }
}
