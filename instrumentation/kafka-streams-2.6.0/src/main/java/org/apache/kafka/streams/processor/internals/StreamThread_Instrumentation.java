package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.LoopState;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.StreamThread")
public class StreamThread_Instrumentation {

    // This method runs once per each event loop iteration
    @Trace(dispatcher = true)
    void runOnce() {
        LoopState.LOCAL.set(new LoopState());
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "KafkaStreams",
                "MessageBroker/Kafka/Streams/EventLoop/Iteration");
        try {
            Weaver.callOriginal();
        } catch (Throwable t) {
            NewRelic.noticeError(t);
            throw t;
        } finally {
            LoopState state = LoopState.LOCAL.get();
            if (state != null && state.getRecordsPolled() == 0 && state.getTotalProcessed() == 0) {
                NewRelic.getAgent().getTransaction().ignore();
            }
            LoopState.LOCAL.remove();
        }

    }

    private ConsumerRecords<byte[], byte[]> pollRequests(final Duration pollTime) {
        ConsumerRecords<byte[], byte[]> records = Weaver.callOriginal();
        LoopState state = LoopState.LOCAL.get();
        if (state != null) {
            int polled = records == null ? 0 : records.count();
            state.setRecordsPolled(polled);
        }
        return records;
    }
}
