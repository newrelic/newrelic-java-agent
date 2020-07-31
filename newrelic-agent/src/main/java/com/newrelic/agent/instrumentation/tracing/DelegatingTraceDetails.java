/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import java.util.List;

import com.newrelic.agent.instrumentation.InstrumentationType;

public class DelegatingTraceDetails implements TraceDetails {
    private final TraceDetails delegate;

    public DelegatingTraceDetails(TraceDetails delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public String metricName() {
        return delegate.metricName();
    }

    @Override
    public boolean dispatcher() {
        return delegate.dispatcher();
    }

    @Override
    public boolean async() {
        return delegate.async();
    }

    @Override
    public String tracerFactoryName() {
        return delegate.tracerFactoryName();
    }

    @Override
    public boolean excludeFromTransactionTrace() {
        return delegate.excludeFromTransactionTrace();
    }

    @Override
    public String metricPrefix() {
        return delegate.metricPrefix();
    }

    @Override
    public String getFullMetricName(String className, String methodName) {
        return delegate.getFullMetricName(className, methodName);
    }

    @Override
    public boolean ignoreTransaction() {
        return delegate.ignoreTransaction();
    }

    @Override
    public boolean isCustom() {
        return delegate.isCustom();
    }

    @Override
    public TransactionName transactionName() {
        return delegate.transactionName();
    }

    @Override
    public List<InstrumentationType> instrumentationTypes() {
        return delegate.instrumentationTypes();
    }

    @Override
    public List<String> instrumentationSourceNames() {
        return delegate.instrumentationSourceNames();
    }

    @Override
    public boolean isWebTransaction() {
        return delegate.isWebTransaction();
    }

    @Override
    public boolean isLeaf() {
        return delegate.isLeaf();
    }

    @Override
    public String[] rollupMetricName() {
        return delegate.rollupMetricName();
    }

    @Override
    public List<ParameterAttributeName> getParameterAttributeNames() {
        return delegate.getParameterAttributeNames();
    }
}
