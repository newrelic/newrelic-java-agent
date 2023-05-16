/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.AgentError;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * The BytecodeGeneratorProxyBuilder is used to create proxy classes that generate bytecode. It builds a proxy that
 * implements a given interface. When methods on the proxy instance are invoked, the proxy will generate the bytecode
 * instructions to invoke the corresponding interface method. The proxy will also load the arguments which have been
 * passed in onto the stack if loadArguments is set to true when the builder is constructed.
 *
 * There is built in support for loading primitive arguments, Strings, enums, and arrays of those types. Additionally,
 * {@link #getVariables()} can be used to load local variables, 'this', and other junk.
 *
 * @param <T>
 * @see Variables
 */
public class BytecodeGenProxyBuilder<T> {

    private final Class<T> target;
    private final GeneratorAdapter methodAdapter;
    private final boolean loadArguments;
    private Map<Object, Runnable> arguments;
    private final Variables variables;
    private Map<Type, VariableLoader> loaders;

    private BytecodeGenProxyBuilder(Class<T> target, GeneratorAdapter methodAdapter, boolean loadArguments) {
        this.target = target;
        this.methodAdapter = methodAdapter;
        this.variables = new VariableLoaderImpl();
        this.loadArguments = loadArguments;
    }

    /**
     * Returns a new api builder. If {@link #loadArguments} is false, the arguments passed when invoking a generated
     * proxy class' methods will be ignored.
     */
    public static <T> BytecodeGenProxyBuilder<T> newBuilder(Class<T> target,
            GeneratorAdapter methodAdapter, boolean loadArguments) {
        return new BytecodeGenProxyBuilder<>(target, methodAdapter, loadArguments);
    }

    public Variables getVariables() {
        return this.variables;
    }

    private BytecodeGenProxyBuilder<T> addArgument(Object instance, Runnable runnable) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        if (runnable == null) {
            throw new AgentError("Runnable was null");
        }
        arguments.put(instance, runnable);
        return this;
    }

    /**
     * Adds a custom variable loader for the given type to extend the built in argument type support.
     */
    public BytecodeGenProxyBuilder<T> addLoader(Type t, VariableLoader loader) {
        if (loaders == null) {
            loaders = new HashMap<>();
        }
        loaders.put(t, loader);
        return this;
    }

    private Map<Type, VariableLoader> getLoaders() {
        return loaders == null ? Collections.<Type, VariableLoader>emptyMap() : loaders;
    }

    private Map<Object, Runnable> getArguments() {
        return arguments == null ? Collections.<Object, Runnable>emptyMap() : arguments;
    }

    /**
     * Returns a proxy implementation that generates bytecode instructions when its methods are invoked.
     */
    @SuppressWarnings("unchecked")
    public T build() {
        InvocationHandler handler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                org.objectweb.asm.commons.Method m = org.objectweb.asm.commons.Method.getMethod(method);

                if (loadArguments) {
                    for (int i = 0; i < m.getArgumentTypes().length; i++) {
                        Object value = args[i];
                        Type type = m.getArgumentTypes()[i];

                        load(type, value);

                    }
                }

                try {
                    getMethodVisitor().visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(target),
                            m.getName(), m.getDescriptor(), true);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Agent.LOG.log(Level.FINER, "Error invoking {0}.{1}", target.getName(), m);
                    throw e;
                }
                return dummyReturnValue(m.getReturnType());
            }

            /**
             * We need to return a dummy value from our handler, but we can't just return null because it'll cause an
             * NPE for primitive types. This covers that.
             *
             * @param returnType
             */
            private Object dummyReturnValue(Type returnType) {
                switch (returnType.getSort()) {
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT:
                        return 0;
                    case Type.LONG:
                        return 0l;
                    case Type.FLOAT:
                        return 0f;
                    case Type.DOUBLE:
                        return 0d;
                    case Type.BOOLEAN:
                        return false;
                }
                return null;
            }

            /**
             * The visitMethodInsn implementation of AdviceAdapter appears to be buggy when the method being visited is
             * a constructor. This works around that by using the mv instance variable of the adapter.
             *
             * This may break in a future release (they also might fix their bug), but we'll know if it breaks because
             * tests will break.
             *
             */
            private MethodVisitor getMethodVisitor() {
                if (methodAdapter instanceof AdviceAdapter) {
                    try {
                        Field field = AdviceAdapter.class.getDeclaredField("isConstructor");
                        field.setAccessible(true);
                        if (field.getBoolean(methodAdapter)) {
                            field = MethodVisitor.class.getDeclaredField("mv");
                            field.setAccessible(true);
                            return (MethodVisitor) field.get(methodAdapter);
                        }
                    } catch (Exception e) {
                        Agent.LOG.log(Level.FINE, e, e.toString());
                    }
                }
                return methodAdapter;
            }

            private void load(Type type, Object value) {
                if (value == null) {
                    methodAdapter.visitInsn(Opcodes.ACONST_NULL);
                    return;
                }

                VariableLoader loader = getLoaders().get(type);
                Runnable handler = getArguments().get(value);

                if (handler != null) {
                    handler.run();
                } else if (loader != null) {
                    loader.load(value, methodAdapter);
                } else if (value instanceof LoaderMarker) {
                    ((LoaderMarker) value).run();
                } else {
                    switch (type.getSort()) {
                        case Type.OBJECT:
                            if (value instanceof String) {
                                methodAdapter.push((String) value);
                            } else if (value.getClass().isEnum()) {
                                @SuppressWarnings("rawtypes")
                                Enum theEnum = (Enum) value;
                                methodAdapter.getStatic(type, theEnum.name(), type);
                            } else if (value instanceof Runnable) {
                                ((Runnable) value).run();
                            } else {
                                throw new AgentError("Unsupported type " + type);
                            }
                            return;
                        case Type.BOOLEAN:
                            methodAdapter.push((Boolean) value);
                            return;
                        case Type.INT:
                            methodAdapter.push(((Number) value).intValue());
                            return;
                        case Type.LONG:
                            methodAdapter.push(((Number) value).longValue());
                            return;
                        case Type.FLOAT:
                            methodAdapter.push(((Number) value).floatValue());
                            return;
                        case Type.DOUBLE:
                            methodAdapter.push(((Number) value).doubleValue());
                            return;
                        case Type.BYTE:
                            methodAdapter.push(((Number) value).intValue());
                            return;
                        case Type.ARRAY:
                            int count = Array.getLength(value);
                            methodAdapter.push(count);
                            methodAdapter.newArray(type.getElementType());

                            for (int i = 0; i < count; i++) {
                                methodAdapter.dup();
                                methodAdapter.push(i);

                                load(type.getElementType(), Array.get(value, i));

                                methodAdapter.arrayStore(type.getElementType());
                            }

                            return;
                        default:
                            throw new AgentError("Unsupported type " + type);
                    }
                }
            }
        };

        ClassLoader classLoader = BytecodeGenProxyBuilder.class.getClassLoader();
        return (T) Proxy.newProxyInstance(classLoader, new Class[] { target }, handler);
    }

    public final class VariableLoaderImpl implements Variables {

        private Runnable loadThis() {
            return new LoaderMarker() {

                @Override
                public void run() {
                    methodAdapter.visitVarInsn(Opcodes.ALOAD, 0);
                }

                @Override
                public String toString() {
                    return "this";
                }

            };
        }

        @Override
        public Object loadThis(int access) {
            boolean isStatic = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
            return isStatic ? null : loadThis();
        }

        @Override
        public <N extends Number> N loadLocal(final int local, final Type type, final N value) {
            Runnable r = () -> methodAdapter.loadLocal(local, type);
            return load(value, r);
        }

        @Override
        public <N extends Number> N load(final N value, final Runnable runnable) {
            addArgument(value, runnable);
            return value;
        }

        @Override
        public Transaction loadCurrentTransaction() {

            return load(Transaction.class, new Runnable() {

                @Override
                public void run() {
                    BridgeUtils.getCurrentTransaction(methodAdapter);
                }

                @Override
                public String toString() {
                    return Transaction.class.getName() + '.' + BridgeUtils.CURRENT_TRANSACTION_FIELD_NAME;
                }

            });
        }

        @SuppressWarnings("unchecked")
        @Override
        public <O> O load(final Class<O> clazz, final Runnable runnable) {

            if (clazz.isInterface()) {
                InvocationHandler handler = new Handler() {

                    @Override
                    public Object doInvoke(Object proxy, Method method, Object[] args) {
                        runnable.run();
                        return null;
                    }

                    @Override
                    public String toString() {
                        return runnable.toString();
                    }

                };

                return (O) Proxy.newProxyInstance(AgentBridge.getAgent().getClass().getClassLoader(), new Class<?>[] { clazz,
                        LoaderMarker.class }, handler);
            } else if (clazz.isArray()) {
                O key = (O) Array.newInstance(clazz.getComponentType(), 0);
                addArgument(key, runnable);
                return key;
            } else if (String.class.equals(clazz)) {
                O key = (O) Long.toString(System.identityHashCode(runnable));
                addArgument(key, runnable);
                return key;
            } else {
                throw new AgentError("Unsupported type " + clazz.getName());
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.newrelic.agent.util.asm.VLoader#loadLocal(int, java.lang.Class)
         */
        @Override
        public <O> O loadLocal(final int localId, final Class<O> clazz) {
            return load(clazz, () -> methodAdapter.loadLocal(localId, Type.getType(clazz)));
        }
    }

    private abstract static class Handler implements InvocationHandler {

        @Override
        public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            } else if (method.getName().equals("toString")) {
                return this.toString();
            }
            return doInvoke(proxy, method, args);
        }

        protected abstract Object doInvoke(Object proxy, Method method, Object[] args);

    }

    /**
     * A marker interface. When a proxy invocation handler is invoked with one of these as an argument we call
     * {@link LoaderMarker#run()}.
     *
     * This shouldn't need to be public but sometimes we see: java.lang.IllegalAccessError: class
     * com.newrelic.agent.util.asm.$Proxy89 cannot access its superinterface
     * com.newrelic.agent.util.asm.ApiBuilder$LoaderMarker
     */
    public interface LoaderMarker extends Runnable {
    }

}
