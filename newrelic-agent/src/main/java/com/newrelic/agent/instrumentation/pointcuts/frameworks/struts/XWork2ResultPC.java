/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.struts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class XWork2ResultPC extends TracerFactoryPointCut {

    public XWork2ResultPC(PointCutClassTransformer classTransformer) {
        super(XWork2ResultPC.class, new InterfaceMatcher("com/opensymphony/xwork2/Result"), createExactMethodMatcher(
                "execute", "(Lcom/opensymphony/xwork2/ActionInvocation;)V"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object result, Object[] args) {
        String name;
        try {
            Object action;
            if (args[0] instanceof ActionInvocation) {
                action = ((ActionInvocation) args[0]).getAction();
            } else {
                action = args[0].getClass().getMethod("getAction").invoke(args[0]);
            }
            name = action.getClass().getName();
        } catch (Throwable t) {
            name = "Unknown";
        }
        return new DefaultTracer(transaction, sig, result, new SimpleMetricNameFormat("StrutsResult/" + name));
    }

}
