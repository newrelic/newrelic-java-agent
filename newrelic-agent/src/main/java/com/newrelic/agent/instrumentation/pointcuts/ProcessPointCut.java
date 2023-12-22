/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

//@PointCut
public class ProcessPointCut extends TracerFactoryPointCut {
    public static final String UNIXPROCESS_CLASS_NAME = "java/lang/UNIXProcess";
    public static final String PROCESS_IMPL_CLASS_NAME = "java/lang/ProcessImpl";

    public ProcessPointCut(PointCutClassTransformer classTransformer) {
        super(ProcessPointCut.class, ExactClassMatcher.or(PROCESS_IMPL_CLASS_NAME, UNIXPROCESS_CLASS_NAME),
                createExactMethodMatcher("waitFor", "()I"));
        classTransformer.getClassNameFilter().addIncludeClass("java/lang/ProcessImpl");
        classTransformer.getClassNameFilter().addIncludeClass("java/lang/UNIXProcess");
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        NewRelic.getAgent().getLogger().log(Level.FINEST,
                "NR709AbsurdValues: inside ProcessPointCut.doGetTracer. tx="+transaction+"; tx.ta="+(transaction == null ? null : transaction.getTransactionActivity()));
        return new DefaultTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig, object));
    }

}
