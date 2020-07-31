/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.lang.reflect.Method;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class HandlerMethodInvokerPointCut extends MethodInvokerPointCut {

    private static final String SPRING_2X_METHOD = "doInvokeMethod";
    private static final String SPRING_3X_METHOD = "resolveHandlerArguments";

    public HandlerMethodInvokerPointCut(PointCutClassTransformer classTransformer) {
        super(
                new ExactClassMatcher("org/springframework/web/bind/annotation/support/HandlerMethodInvoker"),
                OrMethodMatcher.getMethodMatcher(
                // this method is used for spring 2.x to get the name
                // this method is also used for timing in 2.X
                        createExactMethodMatcher(SPRING_2X_METHOD,
                                "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
                        // this method is used for spring 3.x to get the name
                        createExactMethodMatcher(
                                SPRING_3X_METHOD,
                                "(Ljava/lang/reflect/Method;Ljava/lang/Object;Lorg/springframework/web/context/request/NativeWebRequest;Lorg/springframework/ui/ExtendedModelMap;)[Ljava/lang/Object;")));
    }

    @Override
    public Tracer doGetTracer(final Transaction transaction, ClassMethodSignature sig, Object invoker,
            final Object[] args) {

        final String methodName = ((Method) args[0]).getName();
        final Class controller = args[1].getClass();

        setTransactionName(transaction, methodName, controller);

        if (SPRING_3X_METHOD.equals(sig.getMethodName())) {
            // we do not want to time the spring 3X method
            return null;
        } else {
            return new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat("Spring/Java/"
                    + controller.getName() + '/' + methodName)) {

                @Override
                protected void doFinish(Throwable throwable) {
                    // okay, this is our best chance at normalizing this
                    setTransactionName(transaction, methodName, controller);
                }

            };
        }
    }
}
