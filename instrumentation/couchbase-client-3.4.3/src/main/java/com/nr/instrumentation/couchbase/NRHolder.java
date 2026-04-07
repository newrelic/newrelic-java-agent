package com.nr.instrumentation.couchbase;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.Segment;

public class NRHolder {

    private Segment segment = null;
    private DatastoreParameters params = null;
    
    public NRHolder(Segment s, DatastoreParameters p) {
        segment = s;
        params = p;
    }
    
    public void end() {
        if(segment != null) {
            if(params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        }
    }
}
