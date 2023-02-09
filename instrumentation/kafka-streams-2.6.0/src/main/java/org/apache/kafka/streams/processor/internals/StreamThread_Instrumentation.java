package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.StreamsUtil;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.StreamThread")
public class StreamThread_Instrumentation {

    // This method runs once per each event loop iteration
    @Trace(dispatcher = true)
    void runOnce() {
        StreamsUtil.initTransaction();
        try {
            Weaver.callOriginal();
        } catch (Throwable t) {
            NewRelic.noticeError(t);
            throw t;
        } finally {
            StreamsUtil.finalizeLoopState();
        }

    }

    @Trace
    private ConsumerRecords<byte[], byte[]> pollRequests(final Duration pollTime) {
        ConsumerRecords<byte[], byte[]> records = Weaver.callOriginal();
        StreamsUtil.handlePolledRecords(records);
        return records;
    }
}
