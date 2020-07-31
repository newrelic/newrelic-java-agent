/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.TraceMetadata;

public class TraceMetadataImpl implements TraceMetadata {

    public static final TraceMetadata INSTANCE = new TraceMetadataImpl();

    private TraceMetadataImpl() {
    }

    @Override
    public String getTraceId() {
        Transaction tx = TransactionApiImpl.INSTANCE.getTransactionIfExists();
        if (tx == null || !tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            return "";
        }
        return tx.getSpanProxy().getOrCreateTraceId();
    }

    @Override
    public String getSpanId() {
        Transaction tx = TransactionApiImpl.INSTANCE.getTransactionIfExists();
        Tracer tracedMethod = TransactionApiImpl.INSTANCE.getTracedMethodTracer(tx);
        if (tx == null || tracedMethod == null || !tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            return "";
        }
        String spanId = tracedMethod.getGuid();
        return spanId != null ? spanId : "";
    }

    @Override
    public boolean isSampled() {
        Transaction tx = TransactionApiImpl.INSTANCE.getTransactionIfExists();
        if (tx == null || !tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            return false;
        }
        return tx.sampled();
    }
}
