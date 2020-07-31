/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.google.common.collect.ComparisonChain;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.IgnoreTransactionTracerFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

/**
 * PointCuts match certain class/method signatures. If a pointcut positively matches a class method signature, the class
 * byte code is modified so that method invocations are traced. The pointcut returns a {@link TracerFactory} which is
 * used during method invocations to create {@link Tracer}s.
 */
public abstract class PointCut implements Comparable<PointCut>, ClassAndMethodMatcher {
    protected static final int DEFAULT_PRIORITY = 20;
    private final ClassMatcher classMatcher;
    private final MethodMatcher methodMatcher;
    private final PointCutConfiguration config;
    private TracerFactory tracerFactory;
    private int priority = DEFAULT_PRIORITY;
    private final boolean isIgnoreTransaction;

    protected PointCut(PointCutConfiguration config, ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        super();
        assert config != null;
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
        this.config = config;
        isIgnoreTransaction = config.getConfiguration().getProperty("ignore_transaction", false);
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    public boolean isDispatcher() {
        return false;
    }

    public int getPriority() {
        return priority;
    }

    protected void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public final int compareTo(PointCut pc) {
        return ComparisonChain.start()
                .compare(pc.getPriority(), getPriority())
                .compare(getClass().getName(), pc.getClass().getName())
                .result();
    }

    public void noticeTransformerStarted(PointCutClassTransformer classTransformer) {
    }

    protected abstract PointCutInvocationHandler getPointCutInvocationHandlerImpl();

    public final PointCutInvocationHandler getPointCutInvocationHandler() {
        return wrapHandler(isIgnoreTransaction() ? new IgnoreTransactionTracerFactory()
                : getPointCutInvocationHandlerImpl());
    }

    private PointCutInvocationHandler wrapHandler(final PointCutInvocationHandler pointCutInvocationHandler) {
        if (isDispatcher() || !(pointCutInvocationHandler instanceof TracerFactory)) {
            return pointCutInvocationHandler;
        }
        if (tracerFactory == null) {
            tracerFactory = new AbstractTracerFactory() {

                @Override
                public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                        Object[] args) {
                    if (!isDispatcher() && !transaction.isStarted()) {
                        return null;
                    }
                    if (transaction.getTransactionActivity().isLeaf()) {
                        return null;
                    }
                    return ((TracerFactory) pointCutInvocationHandler).getTracer(transaction, sig, object, args);
                }
            };
        }
        return tracerFactory;
    }

    /**
     * True means the transaction should be ignored.
     * 
     * @return True if the transaction should be ignored, else false.
     */
    protected boolean isIgnoreTransaction() {
        return isIgnoreTransaction;
    }

    @Override
    public String toString() {
        return config.getName() == null ? "PointCut:" + getPointCutInvocationHandler().getClass().getName()
                : config.getName();
    }

    /**
     * A convenience method for creating an or method matcher that excludes static methods.
     */
    protected static MethodMatcher createMethodMatcher(MethodMatcher... matchers) {
        return OrMethodMatcher.getMethodMatcher(matchers);
    }

    /**
     * A convenience method for creating an or method matcher that excludes static methods.
     */
    protected static MethodMatcher createExactMethodMatcher(String methodName, String... methodDescriptions) {
        return new ExactMethodMatcher(methodName, methodDescriptions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classMatcher == null) ? 0 : classMatcher.hashCode());
        result = prime * result + ((methodMatcher == null) ? 0 : methodMatcher.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PointCut other = (PointCut) obj;
        if (classMatcher == null) {
            if (other.classMatcher != null) {
                return false;
            }
        } else if (!classMatcher.equals(other.classMatcher)) {
            return false;
        }
        if (methodMatcher == null) {
            if (other.methodMatcher != null) {
                return false;
            }
        } else if (!methodMatcher.equals(other.methodMatcher)) {
            return false;
        }
        return true;
    }

    public String getName() {
        return config.getName();
    }
}
