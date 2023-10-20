/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class SpringDispatcherPointCut extends TracerFactoryPointCut {
    static final String DISPATCHER_SERVLET_CLASS_NAME = "org/springframework/web/servlet/DispatcherServlet";
    private static final String RENDER_METHOD_NAME = "render";

    public SpringDispatcherPointCut(PointCutClassTransformer classTransformer) {
        super(
                SpringDispatcherPointCut.class,
                new ExactClassMatcher(DISPATCHER_SERVLET_CLASS_NAME),
                createMethodMatcher(
                        new ExactMethodMatcher(
                                RENDER_METHOD_NAME,
                                "(Lorg/springframework/web/servlet/ModelAndView;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
                        new ExactMethodMatcher("doDispatch",
                                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
                        new ExactMethodMatcher(
                                RENDER_METHOD_NAME,
                                "(Lorg/springframework/web/servlet/ModelAndView;Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"),
                        new ExactMethodMatcher("doDispatch",
                                "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V")));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher, Object[] args) {
        if (RENDER_METHOD_NAME.equals(sig.getMethodName())) {
            StringBuilder metricName = new StringBuilder("SpringView");
            if (canSetTransactionName(transaction)) {
                try {
                    String viewName = SpringPointCut.getModelAndViewViewName(args[0]);
                    if (viewName != null) {
                        metricName.append(viewName);
                    }
                } catch (Exception e) {
                    metricName.append("/Java/").append(dispatcher.getClass().getName()).append('/').append(
                            sig.getMethodName());
                }
            } else {
                metricName.append("/Java/").append(dispatcher.getClass().getName()).append('/').append(
                        sig.getMethodName());
            }
            return new DefaultTracer(transaction, sig, dispatcher, new SimpleMetricNameFormat(metricName.toString()));
        } else {
            return new DefaultTracer(transaction, sig, dispatcher, new ClassMethodMetricNameFormat(sig, dispatcher));
        }
    }

    private boolean canSetTransactionName(Transaction transaction) {
        return TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy().canSetTransactionName(transaction,
                TransactionNamePriority.FRAMEWORK);
    }

    private void setTransactionName(Transaction transaction, String viewName) {
        if (!transaction.isTransactionNamingEnabled()) {
            return;
        }
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (Agent.LOG.isLoggable(Level.FINER)) {
            if (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Spring view", viewName);
                Agent.LOG.finer(msg);
            }
        }
        policy.setTransactionName(transaction, viewName, SpringPointCut.SPRING_VIEW, TransactionNamePriority.FRAMEWORK);
    }

}
