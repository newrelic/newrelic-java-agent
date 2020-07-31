/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.SkipTracerException;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class BasicRequestRootTracer extends DefaultTracer implements TransactionActivityInitiator {

    private Request request;
    private Response response;

    public BasicRequestRootTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher,
            Request request, final Response response) {
        this(transaction, sig, dispatcher, request, response, new SimpleMetricNameFormat(
                MetricNames.REQUEST_DISPATCHER, ClassMethodMetricNameFormat.getMetricName(sig, dispatcher,
                        MetricNames.REQUEST_DISPATCHER)));
        this.request = request;
        this.response = response;
    }

    public BasicRequestRootTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher,
            Request request, final Response response, MetricNameFormat metricNameFormatter) {
        this(transaction, sig, dispatcher, request, response, metricNameFormatter, DEFAULT_TRACER_FLAGS,
                System.nanoTime());
    }

    public BasicRequestRootTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher,
            Request request, final Response response, MetricNameFormat metricNameFormatter, int flags, long startTime) {
        super(transaction.getTransactionActivity(), sig, dispatcher, metricNameFormatter, flags, startTime);

        this.request = request;
        this.response = response;

        Tracer rootTracer = transaction.getTransactionActivity().getRootTracer();
        if (rootTracer != null) {
            throw new SkipTracerException();
        }
    }

    @Override
    public Dispatcher createDispatcher() {
        return new WebRequestDispatcher(request, response, getTransaction());
    }

    @Override
    protected void reset() {
        super.reset();
    }

    @Override
    protected void doFinish(Throwable throwable) {
        try {
            super.doFinish(throwable);
            getTransaction().setThrowable(throwable, TransactionErrorPriority.TRACER, false);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, "An error occurred calling doFinish() for dispatcher tracer with an exception", e);
        }
    }

}
