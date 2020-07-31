/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.reflect.InvocationHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.bridge.AgentBridge;

public class MethodBuilder {
    static final String INVOCATION_HANDLER_FIELD_NAME = "__nr__InvocationHandlers";
    static final Type INVOCATION_HANDLER_ARRAY_TYPE = Type.getType(InvocationHandler[].class);
    static final Type INVOCATION_HANDLER_TYPE = Type.getType(InvocationHandler.class);
    static final Method INVOCATION_HANDLER_INVOKE_METHOD = new Method("invoke",
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;");

    private final GeneratorAdapter mv;
    private final int access;
    public static final Object LOAD_THIS = new Object();
    public static final Object LOAD_ARG_ARRAY = new Object();

    public MethodBuilder(GeneratorAdapter mv, int access) {
        super();
        this.mv = mv;
        this.access = access;
    }

    public GeneratorAdapter getGeneratorAdapter() {
        return mv;
    }

    /**
     * Loads the AgentWrapper onto the stack by getting it from {@link AgentBridge#agentHandler}
     * 
     */
    public MethodBuilder loadInvocationHandlerFromProxy() {
        // get our InvocationHandler from the private field on the Java Proxy class
        // mv.visitLdcInsn(Type.getType(Proxy.class));

        mv.getStatic(Type.getType(AgentBridge.class), "agentHandler", INVOCATION_HANDLER_TYPE);

        return this;
    }

    /**
     * Invokes {@link InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])}
     * 
     * @param popTheReturnValue if true, the return value of the invoke call will be popped off the stack
     */
    public MethodBuilder invokeInvocationHandlerInterface(boolean popTheReturnValue) {
        mv.invokeInterface(INVOCATION_HANDLER_TYPE, INVOCATION_HANDLER_INVOKE_METHOD);
        if (popTheReturnValue) {
            mv.pop();
        }
        return this;
    }

    /**
     * Loads the first two arguments to a {@link InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])}
     * call onto the stack. The given value is loaded as the first argument, and the second argument (Method) is always
     * null.
     */
    public MethodBuilder loadInvocationHandlerProxyAndMethod(Object value) {
        pushAndBox(value);
        mv.visitInsn(Opcodes.ACONST_NULL); // pass null for the Method argument - it is not used
        return this;
    }

    /**
     * Creates an array containing the objects passed in. If one of the objects is a {@link Runnable},
     * {@link Runnable#run()} will we invoked to push a value onto the stack.
     * 
     * There is built in support for Integers and Booleans. An array object may also be one of the special identifiers
     * on this class like {@link #LOAD_THIS} or {@link #LOAD_ARG_ARRAY}.
     * 
     * @param arrayClass Class of array to create.
     * @param objects
     */
    public MethodBuilder loadArray(Class<?> arrayClass, Object... objects) {
        if (objects == null || objects.length == 0) {
            mv.visitInsn(Opcodes.ACONST_NULL);
            return this;
        }
        mv.push(objects.length);
        Type objectType = Type.getType(arrayClass);
        mv.newArray(objectType);
        for (int i = 0; i < objects.length; i++) {
            mv.dup();
            mv.push(i);
            if (LOAD_THIS == objects[i]) {
                if (isStatic()) {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                } else {
                    mv.loadThis();
                }
            } else if (LOAD_ARG_ARRAY == objects[i]) {
                mv.loadArgArray();
            } else if (objects[i] instanceof Runnable) {
                ((Runnable) objects[i]).run();
            } else {
                pushAndBox(objects[i]);
            }
            mv.arrayStore(objectType);
        }
        return this;
    }

    /**
     * Returns true if the method associated with this builder is static.
     * 
     */
    private boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public MethodBuilder pushAndBox(Object value) {
        if (value == null) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else if (value instanceof Boolean) {
            mv.push((Boolean) value);
            mv.box(Type.BOOLEAN_TYPE);
        } else if (value instanceof Integer) {
            mv.visitIntInsn(Opcodes.SIPUSH, (Integer) value);
            mv.box(Type.INT_TYPE);
        } else {
            mv.visitLdcInsn(value);
        }
        return this;
    }

    /**
     * Calls {@link #loadInvocationHandlerProxyAndMethod(Object)} with the
     * {@link AgentWrapper#SUCCESSFUL_METHOD_INVOCATION} flag.
     * 
     */
    public MethodBuilder loadSuccessful() {
        loadInvocationHandlerProxyAndMethod(AgentWrapper.SUCCESSFUL_METHOD_INVOCATION);
        return this;
    }

    /**
     * Calls {@link #loadInvocationHandlerProxyAndMethod(Object)} with the
     * {@link AgentWrapper#UNSUCCESSFUL_METHOD_INVOCATION} flag.
     * 
     */
    public MethodBuilder loadUnsuccessful() {
        loadInvocationHandlerProxyAndMethod(AgentWrapper.UNSUCCESSFUL_METHOD_INVOCATION);
        return this;
    }

    /**
     * Boxes the value on the top of the stack if the type is a primitive.
     * 
     * @see Integer#valueOf(int)
     * @see Boolean#valueOf(boolean)
     */
    public Type box(Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return type;
        }
        Type boxed = getBoxedType(type);

        mv.invokeStatic(boxed, new Method("valueOf", boxed, new Type[] { type }));

        return boxed;
    }

    /**
     * Returns the object type for a primitive type.
     * 
     * @param type
     */
    public static Type getBoxedType(final Type type) {
        return primitiveToObjectType.get(type);
    }

    private static final Map<Type, Type> primitiveToObjectType = Collections.unmodifiableMap(new HashMap<Type, Type>() {
        private static final long serialVersionUID = 1L;
        {
            put(Type.BOOLEAN_TYPE, Type.getType(Boolean.class));
            put(Type.BYTE_TYPE, Type.getType(Byte.class));
            put(Type.CHAR_TYPE, Type.getType(Character.class));
            put(Type.DOUBLE_TYPE, Type.getType(Double.class));
            put(Type.FLOAT_TYPE, Type.getType(Float.class));
            put(Type.INT_TYPE, Type.getType(Integer.class));
            put(Type.LONG_TYPE, Type.getType(Long.class));
            put(Type.SHORT_TYPE, Type.getType(Short.class));
        }
    });

}
