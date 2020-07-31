/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.custom;

import static com.newrelic.agent.Agent.LOG;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.objectweb.asm.commons.Method;

import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;

public class CustomClassTransformer implements ContextClassTransformer {

    final List<ExtensionClassAndMethodMatcher> extensionPointCuts;
    private final InstrumentationContextManager contextManager;
    private final ClassMatchVisitorFactory matcher;

    public CustomClassTransformer(InstrumentationContextManager contextManager,
            List<ExtensionClassAndMethodMatcher> extensionPointCuts) {
        super();

        this.extensionPointCuts = extensionPointCuts;
        matcher = OptimizedClassMatcherBuilder.newBuilder().addClassMethodMatcher(
                extensionPointCuts.toArray(new ExtensionClassAndMethodMatcher[0])).build();
        contextManager.addContextClassTransformer(matcher, this);
        this.contextManager = contextManager;
    }

    public void destroy() {
        contextManager.removeMatchVisitor(matcher);
    }

    public ClassMatchVisitorFactory getMatcher() {
        return matcher;
    }

    @Override
    public byte[] transform(ClassLoader pLoader, String pClassName, Class<?> pClassBeingRedefined,
            ProtectionDomain pProtectionDomain, byte[] pClassfileBuffer, InstrumentationContext pContext, Match match)
            throws IllegalClassFormatException {
        try {
            if (!PointCutClassTransformer.isValidClassName(pClassName)) {
                return null;
            }

            addMatchesToTraces(pContext, match);
        } catch (Throwable t) {
            LOG.log(Level.FINE, MessageFormat.format("Unable to transform class {0}", pClassName));
            if (LOG.isFinestEnabled()) {
                LOG.log(Level.FINEST, MessageFormat.format("Unable to transform class {0}", pClassName), t);
            }
        }
        return null;
    }

    private void addMatchesToTraces(InstrumentationContext pContext, Match match) {

        Collection<ExtensionClassAndMethodMatcher> matches = new ArrayList<>(extensionPointCuts);
        matches.retainAll(match.getClassMatches().keySet());

        if (!matches.isEmpty()) {
            for (ExtensionClassAndMethodMatcher pc : matches) {
                for (Method m : match.getMethods()) {
                    if (pc.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, m.getName(), m.getDescriptor(),
                            match.getMethodAnnotations(m))) {
                        Method method = pContext.getBridgeMethods().get(m);
                        if (method != null) {
                            // we don't want to instrument bridge methods, we want to instrument the typed
                            // implementation because the bridge method will always call the typed implementation
                            // but direct calls to the typed instrumentation won't pass through the bridge method
                            m = method;
                        }

                        TraceDetails td = pc.getTraceDetails();
                        if (td.ignoreTransaction()) {
                            if (LOG.isFinerEnabled()) {
                                LOG.log(Level.FINER, MessageFormat.format(
                                        "Matched method {0} for ignoring the transaction trace.", m.toString()));
                            }
                            pContext.addIgnoreTransactionMethod(m);
                        } else {
                            if (LOG.isFinerEnabled()) {
                                LOG.log(Level.FINER, MessageFormat.format("Matched method {0} for instrumentation.",
                                        m.toString()));
                            }
                            pContext.addTrace(m, td);
                        }
                    }
                }
            }
        }
    }
}
