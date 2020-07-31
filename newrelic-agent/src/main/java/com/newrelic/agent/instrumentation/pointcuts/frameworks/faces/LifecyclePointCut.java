/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.faces;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class LifecyclePointCut extends TracerFactoryPointCut {
    private static final String METHOD_DESCRIPTION = "(Ljavax/faces/context/FacesContext;)V";

    public LifecyclePointCut(PointCutClassTransformer classTransformer) {
        super(new PointCutConfiguration(LifecyclePointCut.class), OrClassMatcher.getClassMatcher(new ExactClassMatcher(
                "com/sun/faces/lifecycle/LifecycleImpl"), new ExactClassMatcher("com/sun/faces/mock/MockLifecycle"),
                new ChildClassMatcher("javax/faces/lifecycle/Lifecycle")), OrMethodMatcher.getMethodMatcher(
                new ExactMethodMatcher("execute", METHOD_DESCRIPTION), new ExactMethodMatcher("render",
                        METHOD_DESCRIPTION)));
        classTransformer.getClassNameFilter().addIncludeRegex("^(com/sun/faces/|javax/faces/)(.*)");
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object lifecycle, Object[] args) {
        return new DefaultTracer(transaction, sig, lifecycle, new ClassMethodMetricNameFormat(sig, lifecycle));
    }
}
