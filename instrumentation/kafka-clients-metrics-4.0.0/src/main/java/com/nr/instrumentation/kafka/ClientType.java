/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

public enum ClientType {
    CONSUMER("Consume"),
    PRODUCER("Produce");

    private final String operation;

    ClientType(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
