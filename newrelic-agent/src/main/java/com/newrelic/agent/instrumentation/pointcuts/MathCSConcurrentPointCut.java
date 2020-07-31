/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class MathCSConcurrentPointCut extends TracerFactoryPointCut {

    public MathCSConcurrentPointCut(PointCutClassTransformer classTransformer) {
        super(MathCSConcurrentPointCut.class, new InterfaceMatcher(
                "edu/emory/mathcs/backport/java/util/concurrent/Callable"), createExactMethodMatcher("call",
                "()Ljava/lang/Object;"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object callable, Object[] args) {
        return new OtherRootTracer(transaction, sig, callable, new SimpleMetricNameFormat(
                MetricNames.OTHER_TRANSACTION_JOB + "/emoryConcurrentCallable"));
    }

    @Override
    public boolean isDispatcher() {
        return true;
    }
}
