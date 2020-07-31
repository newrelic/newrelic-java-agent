/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import static com.newrelic.agent.model.TransactionTiming.UNASSIGNED_FLOAT;

public class CountedDuration {

    public final static CountedDuration UNASSIGNED = new CountedDuration(UNASSIGNED_FLOAT, UNASSIGNED_FLOAT);
    private final float duration;
    private final float callCount;

    public CountedDuration(float duration, float callCount) {
        this.duration = duration;
        this.callCount = callCount;
    }

    public float getDuration() {
        return duration;
    }

    public float getCallCount() {
        return callCount;
    }
}
