/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.streams;

// Represents a thread-local state for a transaction
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

    public void incRecordsPolled(int recordsPolled) {
        this.recordsPolled += recordsPolled;
    }

    public double getTotalProcessed() {
        return totalProcessed;
    }

    public void incTotalProcessed(double totalProcessed) {
        this.totalProcessed += totalProcessed;
    }
}
