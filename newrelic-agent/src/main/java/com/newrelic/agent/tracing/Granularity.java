/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

public enum Granularity {
    FULL(2.0f ),
    PARTIAL(1.0f);

    private final float sampledPriorityIncrement;

    Granularity(float sampledPriorityIncrement){
        this.sampledPriorityIncrement = sampledPriorityIncrement;
    }

    public float priorityIncrement() {
        return sampledPriorityIncrement;
    }
}
