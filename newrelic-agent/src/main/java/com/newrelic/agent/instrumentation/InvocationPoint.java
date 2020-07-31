/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.logging.Level;

final class InvocationPoint implements InvocationHandler {
    private final TracerFactory tracerFactory;
    private final ClassMethodSignature classMethodSignature;
    private final TracerService tracerService;
    private final boolean ignoreApdex;

    public InvocationPoint(TracerService tracerService, ClassMethodSignature classMethodSignature,
            TracerFactory tracerFactory, boolean ignoreApdex) {
        this.tracerService = tracerService;
        this.tracerFactory = tracerFactory;
        this.classMethodSignature = classMethodSignature;
        this.ignoreApdex = ignoreApdex;
    }

    public ClassMethodSignature getClassMethodSignature() {
        return classMethodSignature;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (ignoreApdex) {
                Transaction transaction = Transaction.getTransaction(false);
                if (transaction != null) {
                    Dispatcher dispatcher = transaction.getDispatcher();
                    if (dispatcher != null) {
                        dispatcher.setIgnoreApdex(true);
                        if (Agent.LOG.isLoggable(Level.FINER)) {
                            String msg = MessageFormat.format("Set Ignore apdex to \"{0}\"", true);
                            Agent.LOG.log(Level.FINER, msg, new Exception());
                        }
                    }
                }
            }

            Object t = tracerService.getTracer(tracerFactory, classMethodSignature, args[0], (Object[]) args[1]);
            return t;
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "Tracer invocation error", t);
        }
        return null;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} {1}", classMethodSignature);
    }

    public TracerFactory getTracerFactory() {
        return tracerFactory;
    }

    public static InvocationHandler getStacklessInvocationHandler(ClassMethodSignature classMethodSignature,
            EntryInvocationHandler tracerFactory) {
        return new StacklessInvocationPoint(classMethodSignature, tracerFactory);
    }

    private static final class StacklessInvocationPoint implements InvocationHandler {
        private final ClassMethodSignature classMethodSignature;
        private final EntryInvocationHandler tracerFactory;

        public StacklessInvocationPoint(ClassMethodSignature classMethodSignature, EntryInvocationHandler tracerFactory) {
            this.classMethodSignature = classMethodSignature;
            this.tracerFactory = tracerFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            tracerFactory.handleInvocation(classMethodSignature, args[0], (Object[]) args[1]);
            return null;
        }
    }

    public static InvocationHandler getInvocationPoint(PointCutInvocationHandler invocationHandler,
            TracerService tracerService, ClassMethodSignature classMethodSignature, boolean ignoreApdex) {
        if (invocationHandler instanceof EntryInvocationHandler) {
            return InvocationPoint.getStacklessInvocationHandler(classMethodSignature,
                    (EntryInvocationHandler) invocationHandler);
        } else if (invocationHandler instanceof TracerFactory) {
            return new InvocationPoint(tracerService, classMethodSignature.intern(), (TracerFactory) invocationHandler,
                    ignoreApdex);
        }
        Agent.LOG.finest("Unable to create an invocation handler for " + invocationHandler);
        if (ignoreApdex) {
            return IgnoreApdexInvocationHandler.INVOCATION_HANDLER;
        }
        return NoOpInvocationHandler.INVOCATION_HANDLER;
    }
}
