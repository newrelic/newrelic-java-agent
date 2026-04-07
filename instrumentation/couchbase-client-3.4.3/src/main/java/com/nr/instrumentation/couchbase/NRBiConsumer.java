package com.nr.instrumentation.couchbase;

import java.util.function.BiConsumer;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

public class NRBiConsumer<V> implements BiConsumer<V, Throwable> {
    
    private Segment segment = null;
    private DatastoreParameters params = null;
    
    public NRBiConsumer(Segment s, DatastoreParameters p) {
        segment = s;
        params = p;
    }

    @Override
    public void accept(V t, Throwable u) {
        if(u != null) {
            NewRelic.noticeError(u);
        }
        if(segment != null) {
            if(params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        }
    }

}
