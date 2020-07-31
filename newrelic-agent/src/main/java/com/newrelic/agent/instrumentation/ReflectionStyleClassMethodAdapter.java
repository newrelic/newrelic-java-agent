/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;

/**
 * The schema of classes which are already loaded cannot be modified with a retransformation. This means that we can't
 * add out InvocationHandler fields to classes which have already loaded when our ClassTransformer is hooked up. In such
 * cases, we instrument the class methods using the older instrumentation approach of getting the tracer by calling
 * {@link AgentWrapper#invoke(Object, java.lang.reflect.Method, Object[])}.
 *
 * This adapter is commonly instrumenting bootstrap classes, but it may encounter non-bootstrap classes too.
 */
public class ReflectionStyleClassMethodAdapter extends AbstractTracingMethodAdapter {
    private final int tracerFactoryId;

    public ReflectionStyleClassMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
            Method method, int tracerFactoryId) {
        super(genericClassAdapter, mv, access, method);
        this.tracerFactoryId = tracerFactoryId;
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.finest("Using fallback instrumentation on " + genericClassAdapter.className + "/" + methodName
                    + methodDesc);
        }
    }

    @Override
    protected void loadGetTracerArguments() {
        methodBuilder.loadInvocationHandlerFromProxy();
        methodBuilder.loadInvocationHandlerProxyAndMethod(tracerFactoryId);
        methodBuilder.loadArray(
        Object.class, // the invocation information
                genericClassAdapter.className, methodName,
                methodDesc,
                // this object
                MethodBuilder.LOAD_THIS, // the arguments
                MethodBuilder.LOAD_ARG_ARRAY);

    }
}
