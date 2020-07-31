/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.interfaces;

import com.newrelic.agent.model.SpanEvent;

/**
 * Stores SpanEvent objects
 */
interface SpanStorage {
    boolean shouldCreateSpan(boolean isTransactionSampled, boolean isCrossProcessTracer, float transactionPriority);

    void storeEvent(SpanEvent spanEvent);

    void start();

    void stop();
}
