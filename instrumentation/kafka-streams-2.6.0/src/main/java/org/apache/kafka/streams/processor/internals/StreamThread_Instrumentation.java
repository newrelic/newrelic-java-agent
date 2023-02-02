package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.StreamThread")
public class StreamThread_Instrumentation {

    // This method runs once per each event loop iteration
    @Trace(dispatcher = true)
    void runOnce() {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "KafkaStreams",
                "MessageBroker/Kafka/Streams/EventLoop/Iteration");
        try {
            Weaver.callOriginal();
        } catch (Throwable t) {
            NewRelic.noticeError(t);
        }

    }
}
