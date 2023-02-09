package org.apache.kafka.streams.processor.internals;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.streams.StreamsUtil;
import org.apache.kafka.common.utils.Time;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.TaskManager")
public class TaskManager_Instrumentation {

    int process(final int maxNumRecords, final Time time) {
        int processed = Weaver.callOriginal();
        StreamsUtil.updateTotalProcessed(processed);
        return processed;
    }
}
