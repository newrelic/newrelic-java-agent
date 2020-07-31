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
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;

/**
 * This class helps us instrument bootstrap classloader classes. Normally our instrumentation fetches a handle to the
 * agent through {@link AgentBridge#getAgent()}, but this won't work since the bootstrap classloader has no visibility to our
 * classes. Also, because the bootstrap classloader has no visibility to our classes it cannot directly invoke any
 * methods on our interfaces and classes even if it has an object reference.
 * 
 * This wrapper is also necessary because some classloader implementations impose rules that strict classloading
 * visibility. OSGi classloaders are a good example.
 * 
 * We work around this problem by sticking a reference to our agent in {@link Proxy}. We use this class as a wrapper so
 * that the object implement {@link InvocationHandler}, which is visible to the bootstrap classloader. The
 * instrumentation code then makes calls to get a tracer and to finish the tracer through this reflection interface.
 */
public class AgentWrapper implements InvocationHandler {

    public static final String CLASSLOADER_KEY = "CLASSLOADER";
    public static final String SUCCESSFUL_METHOD_INVOCATION = "s";
    public static final String UNSUCCESSFUL_METHOD_INVOCATION = "u";

    private final TracerService tracerService;
    private final CoreService coreService;
    private final IAgentLogger logger;
    private final PointCutClassTransformer classTransformer;

    private AgentWrapper(PointCutClassTransformer classTransformer) {
        super();
        tracerService = ServiceFactory.getTracerService();
        this.classTransformer = classTransformer;
        coreService = ServiceFactory.getCoreService();
        logger = Agent.LOG.getChildLogger("com.newrelic.agent.InvocationHandler");
    }

    public static AgentWrapper getAgentWrapper(PointCutClassTransformer classTransformer) {
        return new AgentWrapper(classTransformer);
    }

    /**
     * @see InvocationHandlerTracingMethodAdapter
     * @see ReflectionStyleClassMethodAdapter
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // intentional identity check
        if (CLASSLOADER_KEY == proxy) {
            return AgentBridge.getAgent().getClass().getClassLoader();
        }
        if (!coreService.isEnabled()) {
            return NoOpInvocationHandler.INVOCATION_HANDLER;
        }
        try {
            if (proxy instanceof Class) {
                return createInvocationHandler(proxy, args);
            } else if (proxy instanceof Integer) {
                // we get here if the instrumented class was already loaded before our class transformer initialized
                // see ReflectionStyleClassMethodAdapter
                PointCutInvocationHandler invocationHandler = tracerService.getInvocationHandler((Integer) proxy);
                return invoke(invocationHandler, (String) args[0], (String) args[1], (String) args[2], args[3],
                        (Object[]) args[4]);
            } else {
                logger.log(Level.FINEST, "Unknown invocation type " + proxy);
            }
        } catch (Throwable ex) {
            logger.log(Level.FINEST, "Error initializing invocation point", ex);
        }

        return NoOpInvocationHandler.INVOCATION_HANDLER;
    }

    /**
     * Returns a method tracer invocation handler. This is called by class initializer methods injected by
     * {@link GenericClassAdapter#createNRClassInitMethod()}.
     * 
     * @see InvocationHandlerTracingMethodAdapter
     */
    private Object createInvocationHandler(Object proxy, Object[] args) {
        // this is called by the class constructors we add to instrumented classes
        boolean ignoreTransaction = (Boolean) args[4];
        if (ignoreTransaction) {
            return IgnoreTransactionHandler.IGNORE_TRANSACTION_INVOCATION_HANDLER;
        }
        return classTransformer.evaluate((Class<?>) proxy, tracerService, args[0], args[1], args[2], (Boolean) args[3],
                args);
    }

    /**
     * 
     * @see ReflectionStyleClassMethodAdapter
     */
    public static ExitTracer invoke(PointCutInvocationHandler invocationHandler, String className, String methodName,
            String methodDesc, Object invocationTarget, Object[] args) {

        ClassMethodSignature classMethodSig = new ClassMethodSignature(className, methodName, methodDesc);
        if (invocationHandler instanceof EntryInvocationHandler) {
            EntryInvocationHandler handler = (EntryInvocationHandler) invocationHandler;

            handler.handleInvocation(classMethodSig, invocationTarget, args);
            return null;
        } else if (invocationHandler instanceof TracerFactory) {
            return ServiceFactory.getTracerService().getTracer((TracerFactory) invocationHandler, classMethodSig,
                    invocationTarget, args);
        }

        return null;
    }

    private static class IgnoreTransactionHandler implements InvocationHandler {
        static final InvocationHandler IGNORE_TRANSACTION_INVOCATION_HANDLER = new IgnoreTransactionHandler();

        private IgnoreTransactionHandler() {
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Transaction tx = Transaction.getTransaction(false);
            if (tx != null) {
                tx.setIgnore(true);
            }
            return NoOpInvocationHandler.INVOCATION_HANDLER;
        }
    }
}
