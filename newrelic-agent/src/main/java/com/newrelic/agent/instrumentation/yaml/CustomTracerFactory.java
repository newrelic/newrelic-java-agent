/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.api.agent.MethodTracer;
import com.newrelic.api.agent.MethodTracerFactory;

class CustomTracerFactory extends AbstractTracerFactory {

    private final MethodTracerFactory tracerFactory;

    public CustomTracerFactory(MethodTracerFactory factory) {
        this.tracerFactory = factory;
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        Tracer parent = transaction.getTransactionActivity().getLastTracer();
        final MethodTracer methodTracer = tracerFactory.methodInvoked(sig.getMethodName(), object, args);

        // no custom method tracer, just hook up a normal tracer
        if (methodTracer == null) {
            return parent == null ? new OtherRootTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig,
                    object)) : new DefaultTracer(transaction, sig, object);
        }
        // otherwise we have to let the method tracer know when the method exits
        else {
            // note that while I'd rather wrap both our tracer and the user's tracer with a delegating tracer, our
            // tracer interface has a ton of methods on it and I'm not sure how much other tracers depend on the
            // DefaultTracer. This is the safest way to implement this for now.
            if (parent == null) {
                return new OtherRootTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig, object)) {

                    @Override
                    protected void doFinish(Throwable throwable) {
                        super.doFinish(throwable);
                        methodTracer.methodFinishedWithException(throwable);
                    }

                    @Override
                    protected void doFinish(int opcode, Object returnValue) {
                        super.doFinish(opcode, returnValue);
                        methodTracer.methodFinished(returnValue);
                    }

                };
            } else {
                return new DefaultTracer(transaction, sig, object) {
                    @Override
                    protected void doFinish(Throwable throwable) {
                        super.doFinish(throwable);
                        methodTracer.methodFinishedWithException(throwable);
                    }

                    @Override
                    protected void doFinish(int opcode, Object returnValue) {
                        super.doFinish(opcode, returnValue);
                        methodTracer.methodFinished(returnValue);
                    }
                };
            }
        }
    }

}
