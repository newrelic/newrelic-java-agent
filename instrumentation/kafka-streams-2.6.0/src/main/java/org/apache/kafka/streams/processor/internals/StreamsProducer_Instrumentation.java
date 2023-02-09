package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.StreamsUtil;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.Future;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.StreamsProducer")
public class StreamsProducer_Instrumentation {
    @Trace(leaf = true, excludeFromTransactionTrace = true)
    Future<RecordMetadata> send(final ProducerRecord<byte[], byte[]> record, final Callback callback) {
        StreamsUtil.handleOutgoingRecord(record);
        return Weaver.callOriginal();
    }

}
