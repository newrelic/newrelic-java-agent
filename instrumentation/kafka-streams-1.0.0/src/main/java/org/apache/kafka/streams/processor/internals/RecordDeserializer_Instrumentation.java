package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.processor.ProcessorContext;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.RecordDeserializer")
class RecordDeserializer_Instrumentation {

    ConsumerRecord<Object, Object> deserialize(final ProcessorContext processorContext, final ConsumerRecord<byte[], byte[]> rawRecord) {
        ConsumerRecord<Object, Object> result = Weaver.callOriginal();

        // We need to copy headers from rawRecord to result for 1.x versions
        if (result != null && !result.headers().iterator().hasNext()) {
            for (Header header : rawRecord.headers()) {
                result.headers().add(header);
            }
        }

        return result;
    }
}
