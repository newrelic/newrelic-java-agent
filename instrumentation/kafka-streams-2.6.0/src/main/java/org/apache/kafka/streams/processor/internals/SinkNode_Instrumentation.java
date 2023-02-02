package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.streams.processor.api.Record;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.SinkNode")
public class SinkNode_Instrumentation<KIn, VIn> {

    @Trace
    public void process(final Record<KIn, VIn> record) {
        Segment segment = NewRelic.getAgent()
                .getTransaction()
                .startSegment("SinkProcess");
        try {
            Weaver.callOriginal();
        } finally {
            segment.end();
        }

    }
}
