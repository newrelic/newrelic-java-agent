package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.StateHolder;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.StreamTask")
public class StreamTask_Instrumentation {

    @Trace(dispatcher = true)
    public boolean process(final long wallClockTime) {
        StateHolder.HOLDER.set(new StateHolder());
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            NewRelic.noticeError(e);
            throw e;
        } finally {
            StateHolder holder = StateHolder.HOLDER.get();
            StateHolder.HOLDER.remove();
            if (!holder.isRecordRetrieved()) {
                NewRelic.getAgent().getTransaction().ignore();
            }
        }
    }
}
