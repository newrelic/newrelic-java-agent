package org.apache.kafka.streams.processor.internals;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.StateHolder;
import com.nr.instrumentation.kafka.streams.Util;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.PartitionGroup")
public class PartitionGroup_Instrumentation {

    @Trace // Called from the StreamTask.process() method. Gets the next record to process in a task.
    StampedRecord nextRecord(final PartitionGroup.RecordInfo info, final long wallClockTime) {
        StampedRecord record = Weaver.callOriginal();
        StateHolder holder = StateHolder.HOLDER.get();
        if (AgentBridge.getAgent().getTransaction(false) != null
                && holder != null
                && record != null) {

            holder.setRecordRetrieved(true);
            Util.updateTransaction(record);
        }
        return record;
    }
}
