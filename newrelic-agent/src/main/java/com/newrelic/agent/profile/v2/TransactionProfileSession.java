/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import org.json.simple.JSONStreamAware;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.tracers.Tracer;

/**
 * This tracks extra detail about transactions and instrumentation points
 * to help identify missing instrumentation.  Right now these sessions are
 * started with thread profiles.
 */
public interface TransactionProfileSession extends JSONStreamAware {
    
    /**
     * Called when a transaction finishes.
     * @param transactionData
     */
    void transactionFinished(TransactionData transactionData);

    /**
     * Returns true if this session is active and collecting data.
     */
    boolean isActive();

    /**
     * Called when a tracer is invoked outside of a transaction.
     * @param signatureId
     * @param tracerFlags
     * @param tracer
     */
    void noticeTracerStart(int signatureId, int tracerFlags, Tracer tracer);
}
