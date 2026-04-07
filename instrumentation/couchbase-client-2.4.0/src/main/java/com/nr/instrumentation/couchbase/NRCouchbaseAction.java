package com.nr.instrumentation.couchbase;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import rx.functions.Action0;
import rx.functions.Action1;

public class  NRCouchbaseAction<T extends Throwable> implements Action0, Action1<T> {
    
    private Segment segment = null;
    private ExternalParameters params = null;
    

    
    public NRCouchbaseAction(Segment s, ExternalParameters p) {
        segment = s;
        params = p;
    }

    @Override
    public void call() {
        if(segment != null) {
            if(params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        } else if(params != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
    }

    @Override
    public void call(Throwable t) {
        NewRelic.noticeError(t);
        if(segment != null) {
            if(params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        } else if(params != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
    }

}
