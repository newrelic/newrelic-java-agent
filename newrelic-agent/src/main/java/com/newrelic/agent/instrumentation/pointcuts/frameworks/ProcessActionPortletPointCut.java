/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

/**
 * Instruments portlet 'processAction' invocations.
 */
@PointCut
public class ProcessActionPortletPointCut extends AbstractPortletPointCut {

    public ProcessActionPortletPointCut(PointCutClassTransformer classTransformer) {
        super(ProcessActionPortletPointCut.class, createExactMethodMatcher("processAction",
                "(Ljavax/portlet/ActionRequest;Ljavax/portlet/ActionResponse;)V"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object portlet, Object[] args) {
        return new DefaultTracer(transaction, sig, portlet, new ClassMethodMetricNameFormat(sig, portlet, "Portlet"));
    }

}
