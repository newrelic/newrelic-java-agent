/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class HandleInternalInvokerPointCut extends MethodInvokerPointCut {

    public HandleInternalInvokerPointCut(PointCutClassTransformer classTransformer) {
        super(
                new InterfaceMatcher("org/springframework/web/servlet/HandlerAdapter"),
                OrMethodMatcher.getMethodMatcher(
                        createExactMethodMatcher(
                                "invokeHandleMethod",
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView;"),
                        createExactMethodMatcher(
                                "invokeHandlerMethod",
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView;"),
                        createExactMethodMatcher(
                                "invokeHandleMethod",
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView;"),
                        createExactMethodMatcher(
                                "invokeHandlerMethod",
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView;")));
    }

    @Override
    public Tracer doGetTracer(final Transaction transaction, ClassMethodSignature sig, Object invoker,
            final Object[] args) {

        String methodName = null;
        Class<?> controller = null;
        StringBuilder tracerName = new StringBuilder("Spring/Java");

        // grab the information we need
        try {
            HandlerMethod methodInfo = (HandlerMethod) args[2];

            methodName = methodInfo._nr_getBridgedMethod().getName();
            controller = methodInfo._nr_getBean().getClass();
            // build the tracer name;
            tracerName.append(getControllerName(methodName, controller));

            setTransactionName(transaction, methodName, controller);

        } catch (Exception e) {
            // if it fails use the method name
            Agent.LOG.log(Level.FINE, "Unable to pull controller and method from spring framework.");
            Agent.LOG.log(Level.FINEST, "Exception grabbing spring controller.", e);
            tracerName.append(sig.getMethodName());
        }

        return new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat(tracerName.toString()));
    }

    private String getControllerName(String methodName, Class<?> controller) {
        String controllerName = controller.getName();
        int indexOf = controllerName.indexOf(TO_REMOVE);
        if (indexOf > 0) {
            controllerName = controllerName.substring(0, indexOf);
        }
        return '/' + controllerName + '/' + methodName;
    }
}
