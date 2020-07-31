/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.interfaces;

import com.newrelic.agent.model.SpanEvent;

public class NoOpSpanStorage implements SpanStorage {
    @Override
    public boolean shouldCreateSpan(boolean isTransactionSampled, boolean isCrossProcessTracer, float transactionPriority) {
        return false;
    }

    @Override
    public void storeEvent(SpanEvent spanEvent) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
