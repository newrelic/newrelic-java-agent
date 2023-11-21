/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class HandlerInterceptorPointCut extends TracerFactoryPointCut {
    public HandlerInterceptorPointCut(PointCutClassTransformer classTransformer) {
        super(
                HandlerInterceptorPointCut.class,
                new InterfaceMatcher("org/springframework/web/servlet/HandlerInterceptor"),
                createMethodMatcher(
                        new ExactMethodMatcher("preHandle",
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z"),
                        new ExactMethodMatcher(
                                "postHandle",
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Lorg/springframework/web/servlet/ModelAndView;)V"),
                        new ExactMethodMatcher(
                                "afterCompletion",
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)V"),
                        new ExactMethodMatcher("preHandle",
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z"),
                        new ExactMethodMatcher(
                                "postHandle",
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;Lorg/springframework/web/servlet/ModelAndView;)V"),
                        new ExactMethodMatcher(
                                "afterCompletion",
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)V")));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object interceptor, Object[] args) {
        return new DefaultTracer(transaction, sig, interceptor, new SimpleMetricNameFormat("Spring/HandlerInterceptor",
                ClassMethodMetricNameFormat.getMetricName(sig, interceptor, "Spring/Java")));
    }
}
