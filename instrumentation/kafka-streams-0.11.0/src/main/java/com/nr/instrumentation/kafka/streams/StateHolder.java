/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

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
