package com.nr.instrumentation.couchbase;

import java.util.function.Consumer;

import reactor.core.publisher.SignalType;

public class NRSignalConsumer implements Consumer<SignalType> {
    
    private NRHolder holder = null;
    
    public NRSignalConsumer(NRHolder h) {
        holder = h;
    }

    @Override
    public void accept(SignalType t) {
        if(holder != null) {
            holder.end();
            holder = null;
        }
    }

}
