/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

import java.util.concurrent.TimeUnit;

public class MockTracer extends DefaultTracer {

    private boolean isFinished;
    private long exclusiveDuration;

    public MockTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormat) {
        super(transaction, sig, object);
    }

    public void setExclusiveDurationMs(long ms) {
        this.exclusiveDuration = TimeUnit.MILLISECONDS.toNanos(ms);
    }

    @Override
    public void doFinish(int opcode, Object returnValue) {
        isFinished = true;
    }

    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public long getExclusiveDuration() {
        return exclusiveDuration;
    }

}
