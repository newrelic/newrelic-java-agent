/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.extension.ConfigurationConstruct;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.IgnoreTransactionTracerFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.RetryException;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TracerService extends AbstractService {
    private final Map<String, TracerFactory> tracerFactories = new ConcurrentHashMap<>();

    private volatile PointCutInvocationHandler[] invocationHandlers = new PointCutInvocationHandler[0];
    public ITracerService tracerServiceFactory;

    public TracerService() {
        super(TracerService.class.getSimpleName());
        registerTracerFactory(IgnoreTransactionTracerFactory.TRACER_FACTORY_NAME, new IgnoreTransactionTracerFactory());

        ExtensionService extensionService = ServiceFactory.getExtensionService();
        for (ConfigurationConstruct construct : PointCutFactory.getConstructs()) {
            extensionService.addConstruct(construct);
        }
        tracerServiceFactory = new NoOpTracerService();
    }

    public Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object, Object... args) {
        if (tracerFactory == null) {
            return null;
        }
        return tracerServiceFactory.getTracer(tracerFactory, signature, object, args);
    }

    public TracerFactory getTracerFactory(String tracerFactoryName) {
        return tracerFactories.get(tracerFactoryName);
    }

    public void registerTracerFactory(String name, TracerFactory tracerFactory) {
        tracerFactories.put(name.intern(), tracerFactory);
    }

    /**
     * Registers the PointCutInvocationHandlers. Warning - right now this can only be called once because the order of
     * the {@link #invocationHandlers} array must be preserved.
     */
    public void registerInvocationHandlers(List<PointCutInvocationHandler> handlers) {
        if (invocationHandlers == null) {
            invocationHandlers = handlers.toArray(new PointCutInvocationHandler[handlers.size()]);
        } else {
            PointCutInvocationHandler[] arrayToSwap = new PointCutInvocationHandler[invocationHandlers.length + handlers.size()];
            System.arraycopy(invocationHandlers, 0, arrayToSwap, 0, invocationHandlers.length);
            System.arraycopy(handlers.toArray(), 0, arrayToSwap, invocationHandlers.length, handlers.size());
            invocationHandlers = arrayToSwap;
        }
    }

    /**
     * Returns the id for a given handler to be injected into bytecode for a later call to
     * {@link #getInvocationHandler(int)}.
     *
     * This is slow because we scan linearly for the factory, but this is only called when classes are being
     * instrumented.
     */
    public int getInvocationHandlerId(PointCutInvocationHandler handler) {
        for (int i = 0; i < invocationHandlers.length; i++) {
            if (invocationHandlers[i] == handler) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a InvocationHandler by id.
     */
    public PointCutInvocationHandler getInvocationHandler(int id) {
        return invocationHandlers[id];
    }

    @Override
    protected void doStart() {
        // tracerFactories.put(CustomTracerFactory.class.getName(), new CustomTracerFactory(this));
    }

    @Override
    protected void doStop() {
        tracerFactories.clear();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private interface ITracerService {
        Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object, Object... args);
    }

    private class NoOpTracerService implements ITracerService {

        @Override
        public Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object, Object... args) {
            // it'd be nice if the ServiceManager could just tell us when it starts
            if (ServiceFactory.getServiceManager().isStarted()) {
                TracerService.this.tracerServiceFactory = new TracerServiceImpl();
                return TracerService.this.tracerServiceFactory.getTracer(tracerFactory, signature, object, args);
            }
            return null;
        }
    }

    private class TracerServiceImpl implements ITracerService {
        @Override
        public Tracer getTracer(TracerFactory tracerFactory, ClassMethodSignature signature, Object object, Object... args) {
            Transaction transaction;
            if (tracerFactory instanceof PointCut) {
                PointCut pointCutTracerFactory = (PointCut) tracerFactory;
                transaction = Transaction.getTransaction(pointCutTracerFactory.isDispatcher());
            } else {
                transaction = Transaction.getTransaction(false);
            }
            if (transaction == null) {
                return null;
            }
            try {
                return transaction.getTransactionState().getTracer(transaction, tracerFactory, signature, object, args);
            } catch (RetryException e) {
                return getTracer(tracerFactory, signature, object, args);
            }
        }
    }
}
