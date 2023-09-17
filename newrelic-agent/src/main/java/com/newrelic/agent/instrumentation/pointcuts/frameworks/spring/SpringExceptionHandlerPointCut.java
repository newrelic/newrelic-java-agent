/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.errors.AbstractExceptionHandlerPointCut;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;

@PointCut
public class SpringExceptionHandlerPointCut extends AbstractExceptionHandlerPointCut {

    private static final String PROCESS_HANDLER_EXCEPTION_METHOD_NAME = "processHandlerException";

    public SpringExceptionHandlerPointCut(PointCutClassTransformer classTransformer) {
        super(
                new PointCutConfiguration("spring_exception_handler", "spring_framework", true),
                new ExactClassMatcher(SpringDispatcherPointCut.DISPATCHER_SERVLET_CLASS_NAME),
                createMethodMatcher(
                        new ExactMethodMatcher(
                                PROCESS_HANDLER_EXCEPTION_METHOD_NAME,
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)Lorg/springframework/web/servlet/ModelAndView;"),
                        new ExactMethodMatcher(
                                "triggerAfterCompletion",
                                "(Lorg/springframework/web/servlet/HandlerExecutionChain;ILjavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Exception;)V"),
                        new ExactMethodMatcher(
                                PROCESS_HANDLER_EXCEPTION_METHOD_NAME,
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)Lorg/springframework/web/servlet/ModelAndView;"),
                        new ExactMethodMatcher(
                                "triggerAfterCompletion",
                                "(Lorg/springframework/web/servlet/HandlerExecutionChain;ILjakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljava/lang/Exception;)V")));
    }

    @Override
    protected Throwable getThrowable(ClassMethodSignature sig, Object[] args) {
        int index = PROCESS_HANDLER_EXCEPTION_METHOD_NAME.equals(sig.getMethodName()) ? 3 : 4;
        return (Throwable) args[index];
    }

}
