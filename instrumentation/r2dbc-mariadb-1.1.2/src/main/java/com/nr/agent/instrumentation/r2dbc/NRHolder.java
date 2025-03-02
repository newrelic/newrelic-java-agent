package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import java.util.function.Consumer;

public class NRHolder implements Consumer<Throwable>, Runnable {

    private Segment segment = null;
    private DatastoreParameters params = null;

    public NRHolder(Segment seg, DatastoreParameters p) {
        segment = seg;
        params = p;
    }

    @Override
    public void accept(Throwable t) {
        NewRelic.noticeError(t);
        segment.ignore();
        segment = null;

    }

    @Override
    public void run() {
        if (segment != null) {
            if (params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        }
    }

    public void ignore() {
        if (segment != null) {
            segment.ignore();
            segment = null;
        }
    }

}
