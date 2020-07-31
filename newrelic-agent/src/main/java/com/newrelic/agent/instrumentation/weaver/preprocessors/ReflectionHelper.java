/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.preprocessors;

import com.newrelic.agent.bridge.reflect.ClassReflection;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to rewrite method invocations in weaved classes that require elevated security permissions. Stuff
 * like Class.getField, Field.setAccessible, etc.
 * 
 * @see ClassReflection
 */
class ReflectionHelper {

    private static final ReflectionHelper INSTANCE = new ReflectionHelper();
    private final Map<String, ClassReflector> classes;

    public ReflectionHelper() {
        classes = new HashMap<>();

        for (java.lang.reflect.Method m : ClassReflection.class.getMethods()) {
            Method staticMethod = Method.getMethod(m);

            if (m.getDeclaringClass().equals(ClassReflection.class) && staticMethod.getArgumentTypes().length > 0) {

                Type targetClass = staticMethod.getArgumentTypes()[0];
                Class<?>[] args = new Class[m.getParameterTypes().length - 1];
                System.arraycopy(m.getParameterTypes(), 1, args, 0, staticMethod.getArgumentTypes().length - 1);

                try {
                    // make sure the method exists on the target class.
                    m.getParameterTypes()[0].getMethod(m.getName(), args);

                    ClassReflector classReflector = classes.get(targetClass.getInternalName());
                    if (classReflector == null) {
                        classReflector = new ClassReflector();
                        classes.put(targetClass.getInternalName(), classReflector);
                    }

                    Type[] argumentTypes = new Type[staticMethod.getArgumentTypes().length - 1];
                    System.arraycopy(staticMethod.getArgumentTypes(), 1, argumentTypes, 0,
                            staticMethod.getArgumentTypes().length - 1);

                    Method targetMethod = new Method(m.getName(), staticMethod.getReturnType(), argumentTypes);
                    classReflector.methods.put(targetMethod, staticMethod);
                } catch (NoSuchMethodException ex) {
                    // ignore
                }
            }
        }
    }

    private static class ClassReflector {

        private final Map<Method, Method> methods = new HashMap<>();

        public ClassReflector() {
        }

    }

    public static ReflectionHelper get() {
        return INSTANCE;
    }

    /**
     * Replace invocations of some reflective apis with static method invocations.
     */
    public boolean process(String owner, String name, String desc, GeneratorAdapter generatorAdapter) {
        ClassReflector classReflector = classes.get(owner);
        if (classReflector != null) {
            Method method = classReflector.methods.get(new Method(name, desc));
            if (method != null) {
                // we're replacing a virtual invocation with a static invocation, but the first arg is the invokee so
                // everything lines up
                generatorAdapter.invokeStatic(Type.getType(ClassReflection.class), method);
                return true;
            }
        }
        return false;
    }
}
