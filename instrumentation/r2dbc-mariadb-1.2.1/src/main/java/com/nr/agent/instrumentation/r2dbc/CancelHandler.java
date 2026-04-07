package com.nr.agent.instrumentation.r2dbc;

public class CancelHandler implements Runnable {

    private NRHolder holder = null;

    public CancelHandler(NRHolder hold) {
        holder = hold;
    }

    @Override
    public void run() {
        if (holder != null) {
            holder.ignore();
        }
    }

}
