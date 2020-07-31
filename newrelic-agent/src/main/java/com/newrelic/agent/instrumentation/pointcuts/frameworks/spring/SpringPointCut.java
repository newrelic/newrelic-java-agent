/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class SpringPointCut extends TracerFactoryPointCut {
    public static final String SPRING_CONTROLLER = "SpringController";
    public static final String SPRING_VIEW = "SpringView";

    private static final String REDIRECT_VIEW_SYNTAX = "/redirect:";
    private static final String FORWARD_VIEW_SYNTAX = "/forward:";
    private static final Pattern HTTP_PATTERN = Pattern.compile("(.*)https?://.*");
    private final AgentConfig config;

    public SpringPointCut(PointCutClassTransformer pointCutClassTransformer, AgentConfig config) {
        super(SpringPointCut.class,
        // new OrClassMatcher(
                new InterfaceMatcher("org/springframework/web/servlet/HandlerAdapter"),
                // new InterfaceMatcher("org/springframework/web/servlet/mvc/Controller"),
                // new ChildClassMatcher("org/springframework/web/servlet/mvc/multiaction/MultiActionController",
                // false)),
                // OrMethodMatcher.getMethodMatcher(
                // new ExactMethodMatcher("handleRequest",
                // "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;")));
                new ExactMethodMatcher(
                        "handle",
                        "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Lorg/springframework/web/servlet/ModelAndView;"));
        // new ExactMethodMatcher("handleRequest",
        // "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Lorg/springframework/web/servlet/ModelAndView;"),
        // new ExactMethodMatcher("handleRequest",
        // "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;"),
        // new ExactMethodMatcher("invokeNamedMethod",
        // "(Ljava/lang/String;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;"))));
        this.config = config;
    }

    public SpringPointCut(PointCutClassTransformer pointCutClassTransformer) {
        this(pointCutClassTransformer, ServiceFactory.getConfigService().getDefaultAgentConfig());
    }

    static String getModelAndViewViewName(Object modelAndView) throws IllegalArgumentException, SecurityException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String viewName;
        if (modelAndView instanceof ModelAndView) {
            viewName = ((ModelAndView) modelAndView).getViewName();
        } else {
            viewName = (String) modelAndView.getClass().getMethod("getViewName").invoke(modelAndView);
        }
        return cleanModelAndViewName(viewName);
    }

    static String cleanModelAndViewName(String viewName) {
        if (viewName == null || viewName.length() == 0) {
            return viewName;
        }
        if (viewName.charAt(0) != '/') {
            viewName = '/' + viewName;
        }
        if (viewName.startsWith(REDIRECT_VIEW_SYNTAX)) {
            return "/redirect:*";
        }
        if (viewName.startsWith(FORWARD_VIEW_SYNTAX)) {
            // Let forwards be named after their destination view
            return null;// "/forward:*";
        }

        viewName = ServiceFactory.getNormalizationService().getUrlBeforeParameters(viewName);

        Matcher paramDelimiterMatcher = HTTP_PATTERN.matcher(viewName);
        if (paramDelimiterMatcher.matches()) {
            viewName = paramDelimiterMatcher.group(1) + '*';
        }
        return viewName;
    }

    @Override
    public Tracer doGetTracer(final Transaction transaction, final ClassMethodSignature sig, final Object controller,
            Object[] args) {
        final Object handler = args[2];
        return new DefaultTracer(transaction, sig, controller) {

            @Override
            protected void doFinish(int opcode, Object modelView) {
                String metricName;
                if (handler != null) {
                    StringBuilder tracerName = new StringBuilder("SpringController/");
                    tracerName.append(getControllerName(handler.getClass()));
                    metricName = tracerName.toString();
                } else {
                    StringBuilder tracerName = new StringBuilder("SpringController/");
                    tracerName.append(getControllerName(controller.getClass()));
                    tracerName.append('/').append(sig.getMethodName());
                    metricName = tracerName.toString();
                }
                setMetricNameFormat(new SimpleMetricNameFormat(metricName));
                super.doFinish(opcode, modelView);
            }

            private String getControllerName(Class<?> controller) {
                String controllerName = controller.getName();
                int indexOf = controllerName.indexOf(MethodInvokerPointCut.TO_REMOVE);
                if (indexOf > 0) {
                    controllerName = controllerName.substring(0, indexOf);
                }
                return controllerName;
            }

            private void setTransactionName(Transaction transaction, Object modelView) {
                if (!transaction.isTransactionNamingEnabled()) {
                    return;
                }

                TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
                if (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK)) {
                    String modelAndViewName = doGetModelAndViewName(modelView);
                    if (modelAndViewName == null) {
                        return;
                    }
                    if (Agent.LOG.isLoggable(Level.FINER)) {
                        String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Spring ModelView",
                                modelAndViewName);
                        Agent.LOG.finer(msg);
                    }
                    policy.setTransactionName(transaction, modelAndViewName, SPRING_VIEW,
                            TransactionNamePriority.FRAMEWORK);
                }
            }

            private String doGetModelAndViewName(Object modelAndView) {
                try {
                    return getModelAndViewViewName(modelAndView);
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINE, "Unable to parse Spring ModelView", e);
                }
                return null;
            }

        };
    }

    @Override
    public boolean isEnabled() {
        boolean defaultInstrumentationEnabled = config.getClassTransformerConfig().isDefaultInstrumentationEnabled();
        boolean springPointCutExplicitlyEnabled = config.getProperty("enable_spring_tracing", false);
        boolean springPointcutExplicitlyDisabled = !config.getProperty("enable_spring_tracing", true);

        if (springPointCutExplicitlyEnabled) {
            return true;
        } else if (springPointcutExplicitlyDisabled) {
            return false;
        }

        return defaultInstrumentationEnabled;
    }
}
