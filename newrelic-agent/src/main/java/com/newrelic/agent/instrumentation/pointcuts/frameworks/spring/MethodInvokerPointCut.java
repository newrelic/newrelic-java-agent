/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public abstract class MethodInvokerPointCut extends TracerFactoryPointCut {
    private static final String SPRING_FRAMEWORK_CONFIG_PARAMETER_NAME = "spring_framework";

    protected static final String TO_REMOVE = "$$EnhancerBy"; // EnhancerByCGLIB or EnhancerBySpringCGLIB

    private final boolean useFullPackageName;

    public MethodInvokerPointCut(ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        super(new PointCutConfiguration("spring_handler_method_invoker"), classMatcher, methodMatcher);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        useFullPackageName = getSpringConfiguration(config).getProperty("use_full_package_name", false);
    }

    /**
     * Returns the Spring configuration section (instrumentation=>spring_framework).
     * 
     * @param config
     */
    private static BaseConfig getSpringConfiguration(AgentConfig config) {
        Map<String, Object> props = config.getInstrumentationConfig().getProperty(
                SPRING_FRAMEWORK_CONFIG_PARAMETER_NAME, new HashMap<String, Object>());
        return new BaseConfig(props);
    }


    /**
     * Gets the field useFullPackageName.
     * 
     * @return the useFullPackageName
     */
    protected boolean isUseFullPackageName() {
        return useFullPackageName;
    }

    protected void setTransactionName(Transaction transaction, String methodName, Class<?> pController) {
        if (!transaction.isTransactionNamingEnabled()) {
            return;
        }

        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK)) {
            String controller = getControllerName(methodName, pController);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Spring controller",
                        controller);
                Agent.LOG.finer(msg);
            }
            policy.setTransactionName(transaction, controller, SpringPointCut.SPRING_CONTROLLER,
                    TransactionNamePriority.FRAMEWORK);
        }
    }

    private String getControllerName(String methodName, Class<?> controller) {
        Class<?> originalClass = getOriginalClassIfProxied(controller);
        String controllerName = isUseFullPackageName() ? originalClass.getName() : originalClass.getSimpleName();
        return '/' + controllerName + '/' + methodName;
    }

    private Class<?> getOriginalClassIfProxied(Class<?> clazz) {
        if (clazz.getName().contains("$$")) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }

        return clazz;
    }
}
