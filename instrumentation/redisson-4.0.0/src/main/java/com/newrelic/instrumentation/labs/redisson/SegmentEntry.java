package com.newrelic.instrumentation.labs.redisson;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.Segment;

public class SegmentEntry {
    private Segment segment;
    private DatastoreParameters params;

    public SegmentEntry(Segment segment, DatastoreParameters params) {
        this.segment = segment;
        this.params = params;
    }

    public Segment getSegment() {
        return segment;
    }

    public DatastoreParameters getParams() {
        return params;
    }

    public void clear() {
        segment = null;
        params = null;
    }
}
