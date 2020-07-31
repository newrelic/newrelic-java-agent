/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.struts;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

/**
 * Instruments the execute method of Struts actions.
 */
@PointCut
public class StrutsActionPointCut extends TracerFactoryPointCut {
    public static final String STRUTS_ACTION_CLASS = "org/apache/struts/action/Action";
    private static final MethodMatcher METHOD_MATCHER = createExactMethodMatcher(
            "execute",
            "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)Lorg/apache/struts/action/ActionForward;",
            "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/apache/struts/action/ActionForward;");

    public StrutsActionPointCut(PointCutClassTransformer classTransformer) {
        super(StrutsActionPointCut.class, new ChildClassMatcher(STRUTS_ACTION_CLASS), METHOD_MATCHER);
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object action, Object[] args) {
        return new StrutsActionTracer(transaction, sig, action, args);
    }

    private static class StrutsActionTracer extends DefaultTracer {
        private final String actionClassName;

        public StrutsActionTracer(Transaction transaction, ClassMethodSignature sig, Object action, Object[] args) {
            super(transaction, sig, action);
            actionClassName = action.getClass().getName();
            setTransactionName(transaction, actionClassName);
            setMetricNameFormat(new SimpleMetricNameFormat("StrutsAction/" + actionClassName));
        }

        private void setTransactionName(Transaction tx, String action) {
            if (!tx.isTransactionNamingEnabled()) {
                return;
            }
            TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
            if (Agent.LOG.isLoggable(Level.FINER)) {
                if (policy.canSetTransactionName(tx, TransactionNamePriority.FRAMEWORK)) {
                    String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Struts action", action);
                    Agent.LOG.finer(msg);
                }
            }
            policy.setTransactionName(tx, action, MetricNames.STRUTS_ACTION, TransactionNamePriority.FRAMEWORK);
        }

    }

}
