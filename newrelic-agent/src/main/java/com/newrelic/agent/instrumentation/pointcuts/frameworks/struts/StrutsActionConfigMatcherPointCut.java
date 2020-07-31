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
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.util.Invoker;

@PointCut
public class StrutsActionConfigMatcherPointCut extends TracerFactoryPointCut {
    private static final String STRUTS = "Struts";
    private static final String ACTION_CONFIG_MATCHER_CLASS = "org/apache/struts/config/ActionConfigMatcher";
    private static final String GET_PATH = "getPath";

    public StrutsActionConfigMatcherPointCut(PointCutClassTransformer classTransformer) {
        super(
                StrutsActionConfigMatcherPointCut.class,
                new ExactClassMatcher(ACTION_CONFIG_MATCHER_CLASS),
                createExactMethodMatcher("convertActionConfig",
                        "(Ljava/lang/String;Lorg/apache/struts/config/ActionConfig;Ljava/util/Map;)Lorg/apache/struts/config/ActionConfig;"));
    }

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object matcher, Object[] args) {
        return new StrutsActionConfigMatcherTracer(transaction, sig, matcher, args);
    }

    private static class StrutsActionConfigMatcherTracer extends MethodExitTracer {
        public StrutsActionConfigMatcherTracer(Transaction transaction, ClassMethodSignature sig, Object matcher,
                Object[] args) {
            super(sig, transaction);
            try {
                Object actionConfig = args[1];
                String wildcardPath = (String) Invoker.invoke(actionConfig, actionConfig.getClass(), GET_PATH);
                Agent.LOG.finer("Normalizing path using Struts wildcard");
                setTransactionName(transaction, wildcardPath);
            } catch (Exception e) {
                String msg = MessageFormat.format("Exception in {0} handling {1}: {2}",
                        StrutsActionConfigMatcherPointCut.class.getSimpleName(), sig, e);
                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, e);
                } else {
                    Agent.LOG.finer(msg);
                }
            }
        }

        private void setTransactionName(Transaction transaction, String wildcardPath) {
            if (!transaction.isTransactionNamingEnabled()) {
                return;
            }
            TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
            if (Agent.LOG.isLoggable(Level.FINER)) {
                if (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK)) {
                    String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Struts wildcard",
                            wildcardPath);
                    Agent.LOG.finer(msg);
                }
            }
            policy.setTransactionName(transaction, wildcardPath, STRUTS, TransactionNamePriority.FRAMEWORK);
        }

        @Override
        protected void doFinish(int opcode, Object returnValue) {
            // no op
        }

    }
}
