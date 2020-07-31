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
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

/**
 * Instruments the execute method of Struts 2 actions.
 */
@PointCut
public class Struts2ActionPointCut extends TracerFactoryPointCut {
    public static final String STRUTS_ACTION__PROXY_INTERFACE = "com/opensymphony/xwork2/ActionProxy";
    private static final MethodMatcher METHOD_MATCHER = createExactMethodMatcher("execute", "()Ljava/lang/String;");

    public Struts2ActionPointCut(PointCutClassTransformer classTransformer) {
        super(Struts2ActionPointCut.class, new InterfaceMatcher(STRUTS_ACTION__PROXY_INTERFACE), METHOD_MATCHER);
    }

    @Override
    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object action, Object[] args) {
        try {
            String realAction;
            if (action instanceof ActionProxy) {
                realAction = ((ActionProxy) action).getActionName();
            } else {
                realAction = (String) action.getClass().getMethod("getActionName").invoke(action);
            }
            setTransactionName(tx, realAction);
            return new DefaultTracer(tx, sig, action, new SimpleMetricNameFormat(MetricNames.STRUTS_ACTION_PREFIX
                    + realAction));
        } catch (Exception e) {
            return new DefaultTracer(tx, sig, action, new ClassMethodMetricNameFormat(sig, action,
                    MetricNames.STRUTS_ACTION));
        }
    }

    private void setTransactionName(Transaction tx, String action) {
        if (!tx.isTransactionNamingEnabled()) {
            return;
        }
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (Agent.LOG.isLoggable(Level.FINER)) {
            if (policy.canSetTransactionName(tx, TransactionNamePriority.FRAMEWORK)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Struts 2 action", action);
                Agent.LOG.finer(msg);
            }
        }
        policy.setTransactionName(tx, action, MetricNames.STRUTS_ACTION, TransactionNamePriority.FRAMEWORK);
    }

}
