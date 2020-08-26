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
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

/**
 * This point cut is here for spring 3.x. However, it will also be hit with spring 2.X.
 */
@PointCut
public class HandlerMethodInvoker3PointCut extends TracerFactoryPointCut {

    public HandlerMethodInvoker3PointCut(PointCutClassTransformer classTransformer) {
        super(
                new PointCutConfiguration("spring_handler_method_invoker"),
                new ExactClassMatcher("org/springframework/web/bind/annotation/support/HandlerMethodInvoker"),
                createExactMethodMatcher(
                        "invokeHandlerMethod",
                        "(Ljava/lang/reflect/Method;Ljava/lang/Object;Lorg/springframework/web/context/request/NativeWebRequest;Lorg/springframework/ui/ExtendedModelMap;)Ljava/lang/Object;"));
    }

    @Override
    public Tracer doGetTracer(final Transaction transaction, ClassMethodSignature sig, Object invoker,
            final Object[] args) {
        StringBuilder tracerName = new StringBuilder("Spring/Java");
        String methodName = ((Method) args[0]).getName();
        Class<?> controller = args[1].getClass();
        // build the tracer name;
        tracerName.append(getControllerName(methodName, controller));
        Tracer result = new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat(tracerName.toString()));
        result.setInstrumentationModule("spring-pointcut");
        return result;
    }

    private String getControllerName(String methodName, Class<?> controller) {
        String controllerName = controller.getName();
        int indexOf = controllerName.indexOf(MethodInvokerPointCut.TO_REMOVE);
        if (indexOf > 0) {
            controllerName = controllerName.substring(0, indexOf);
        }
        return '/' + controllerName + '/' + methodName;
    }
}
