package com.nr.instrumentation.kafka.streams;

public class StateHolder {
    public static ThreadLocal<StateHolder> HOLDER = new ThreadLocal<>();

    private boolean recordRetrieved = false;

    public boolean isRecordRetrieved() {
        return recordRetrieved;
    }

    public void setRecordRetrieved(boolean recordRetrieved) {
        this.recordRetrieved = recordRetrieved;
    }
}
