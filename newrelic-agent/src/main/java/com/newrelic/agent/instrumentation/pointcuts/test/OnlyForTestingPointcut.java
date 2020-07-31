/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.test;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

/**
 * This is used by RetransformPointCutTest.
 */
@PointCut
public class OnlyForTestingPointcut extends TracerFactoryPointCut {
    private final int METRIC_NAME_ARGUMENT = 2;

    public OnlyForTestingPointcut(PointCutClassTransformer ct) {
        super(OnlyForTestingPointcut.class, new ExactClassMatcher("com/newrelic/api/jruby/DoNothingClassThatExistsForTesting"),
                new ExactMethodMatcher("trace"));
    }

    @Override
    public Tracer doGetTracer(final Transaction transaction, final ClassMethodSignature sig, final Object object,
            Object[] args) {
        String metric = args[METRIC_NAME_ARGUMENT].toString();
        return new DefaultTracer(transaction, sig, object, new SimpleMetricNameFormat(metric));
    }

}