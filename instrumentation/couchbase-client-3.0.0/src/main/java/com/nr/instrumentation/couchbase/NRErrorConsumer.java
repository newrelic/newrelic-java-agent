package com.nr.instrumentation.couchbase;

import java.util.function.Consumer;

public class NRErrorConsumer implements Consumer<Throwable> {
    
    private NRHolder holder = null;
    
    public NRErrorConsumer(NRHolder h) {
        holder = h;
    }

    @Override
    public void accept(Throwable t) {
        if(holder != null) {
            holder.end();
            holder = null;
        }
    }

}
