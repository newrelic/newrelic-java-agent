package com.nr.instrumentation.kafka.streams;

public class LoopState {
    public final static ThreadLocal<LoopState> LOCAL = new ThreadLocal<>();
    private int recordsPolled;
    private double totalProcessed;

    public LoopState() {
        clear();
    }

    public void clear() {
        recordsPolled = 0;
        totalProcessed = 0;
    }

    public int getRecordsPolled() {
        return recordsPolled;
    }

    public void setRecordsPolled(int recordsPolled) {
        this.recordsPolled = recordsPolled;
    }

    public double getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(double totalProcessed) {
        this.totalProcessed = totalProcessed;
    }
}
