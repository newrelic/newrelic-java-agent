/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;


/**
 * This method adapter fetches the method tracer by invoking the handler stored in the static field named
 * {@link MethodBuilder#INVOCATION_HANDLER_FIELD_NAME}.
 */
public class InvocationHandlerTracingMethodAdapter extends AbstractTracingMethodAdapter {

    public InvocationHandlerTracingMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
            Method method) {
        super(genericClassAdapter, mv, access, method);
    }

    @Override
    protected void loadGetTracerArguments() {
        getStatic(Type.getObjectType(genericClassAdapter.className), MethodBuilder.INVOCATION_HANDLER_FIELD_NAME,
                MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE);

        push(getInvocationHandlerIndex());
        arrayLoad(getTracerType());

        methodBuilder
                // we pass null to the first two arguments of our InvocationHandler
                .loadInvocationHandlerProxyAndMethod(null)
                // now we pass all of the arguments needed to get a tracer
                .loadArray(Object.class, MethodBuilder.LOAD_THIS, MethodBuilder.LOAD_ARG_ARRAY);
    }

}
