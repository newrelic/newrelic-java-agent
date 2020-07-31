/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.container.tomcat;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

/**
 * This pointcut instruments the method in Glassfish and Tomcat 6 or later which prepares the response. This is the last
 * chance to add the cross process response header. For Tomcat 5, @see #FinishResponsePointCut.
 */
@PointCut
public class PrepareResponsePointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {

    private static final String POINT_CUT_NAME = PrepareResponsePointCut.class.getName();
    private static final boolean DEFAULT_ENABLED = true;
    // Tomcat 7
    private static final String COYOTE_ABSTRACT_HTTP11_PROCESSOR_CLASS = "org/apache/coyote/http11/AbstractHttp11Processor";
    // Tomcat 6
    private static final String COYOTE_HTTP11_PROCESSOR_CLASS = "org/apache/coyote/http11/Http11Processor";
    // Glassfish
    private static final String GRIZZLY_PROCESSOR_TASK_CLASS = "com/sun/grizzly/http/ProcessorTask";

    private static final String PREPARE_RESPONSE_METHOD_NAME = "prepareResponse";
    private static final String PREPARE_RESPONSE_METHOD_DESC = "()V";

    public PrepareResponsePointCut(PointCutClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "tomcat",
                DEFAULT_ENABLED);
    }

    private static ClassMatcher createClassMatcher() {
        return ExactClassMatcher.or(COYOTE_ABSTRACT_HTTP11_PROCESSOR_CLASS, COYOTE_HTTP11_PROCESSOR_CLASS,
                GRIZZLY_PROCESSOR_TASK_CLASS);
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher(PREPARE_RESPONSE_METHOD_NAME, PREPARE_RESPONSE_METHOD_DESC);
    }

    @Override
    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    @Override
    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx == null) {
            return;
        }
        tx.addOutboundResponseHeaders();
    }
}
