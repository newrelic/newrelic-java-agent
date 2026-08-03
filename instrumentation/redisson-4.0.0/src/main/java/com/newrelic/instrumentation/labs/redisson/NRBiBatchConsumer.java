package com.newrelic.instrumentation.labs.redisson;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;

import java.util.List;
import java.util.function.BiConsumer;

public class NRBiBatchConsumer<T> implements BiConsumer<T, Throwable> {

    private final List<SegmentEntry> segmentEntries;

    public NRBiBatchConsumer(List<SegmentEntry> segmentEntries) {
        super();
        this.segmentEntries = segmentEntries;
    }

    @Override
    @Trace
    public void accept(T t, Throwable u) {
        if(u != null) {
            NewRelic.noticeError(u);
        }

        for (SegmentEntry entry : segmentEntries) {
            Segment segment = entry.getSegment();
            DatastoreParameters params = entry.getParams();
            if(segment != null) {
                if(params != null) {
                    segment.reportAsExternal(params);
                }
                segment.end();
                entry.clear();
            }
        }
        segmentEntries.clear();


    }

}
