/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Visit a class to add method mappers to it.
 */
public class MethodMappersAdapter extends ClassVisitor {

    /*
     * Key is the method mapper; value is the mapped method.
     * 
     * A method mapper is a method to be added to the visited class that invokes an existing mapped method.
     */
    private final Map<Method, java.lang.reflect.Method> methods;
    private final String className;
    private final String originalInterface;

    /**
     * @param cv The ClassVisitor to delegate to
     * @param methods The method mappers
     * @param type The name of the interface the mapped methods belong to
     * @param className The name of the visited class
     */
    private MethodMappersAdapter(ClassVisitor cv, Map<Method, java.lang.reflect.Method> methods,
            String originalInterface, String className) {
        super(WeaveUtils.ASM_API_LEVEL, cv);
        this.methods = methods;
        this.originalInterface = originalInterface;
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Method originalMethod = new Method(name, desc);
        java.lang.reflect.Method method = methods.remove(originalMethod);
        if (method != null) {
            addMethod(method, originalMethod);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private void addMethod(java.lang.reflect.Method method, Method originalMethod) {
        Method newMethod = InstrumentationUtils.getMethod(method);
        MethodMapper methodMapper = method.getAnnotation(MethodMapper.class);

        Type returnType = Type.getType(method.getReturnType());
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC, newMethod, null, null, this);
        mv.visitCode();

        mv.loadThis();

        for (int i = 0; i < newMethod.getArgumentTypes().length; i++) {
            mv.loadArg(i);
        }

        if (methodMapper.invokeInterface()) {
            mv.invokeInterface(Type.getObjectType(originalInterface), originalMethod);
        } else {
            mv.invokeVirtual(Type.getObjectType(className), originalMethod);
        }
        mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
    }

    protected static Map<Method, java.lang.reflect.Method> getMethodMappers(Class<?> type) {
        Map<Method, java.lang.reflect.Method> methods = new HashMap<>();
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            MethodMapper annotation = method.getAnnotation(MethodMapper.class);
            if (annotation == null) {
                throw new RuntimeException("Method " + method.getName() + " does not have a MethodMapper annotation");
            }
            String originalMethodName = annotation.originalMethodName();
            String originalDescriptor = annotation.originalDescriptor();
            if (MethodMapper.NULL.equals(originalDescriptor)) {
                originalDescriptor = InstrumentationUtils.getMethod(method).getDescriptor();
            }
            if (method.getName().equals(annotation.originalMethodName())) {
                String msg = MessageFormat.format(
                        "Ignoring {0} method in {1}: method name is same as orginalMethodName", method.getName(),
                        type.getClass().getName());
                Agent.LOG.fine(msg);
            } else {
                methods.put(new Method(originalMethodName, originalDescriptor), method);
            }
        }
        return methods;
    }

    public static MethodMappersAdapter getMethodMappersAdapter(ClassVisitor cv,
            Map<Method, java.lang.reflect.Method> methods, String originalInterface, String className) {
        Map<Method, java.lang.reflect.Method> methods2 = new HashMap<>(methods);
        return new MethodMappersAdapter(cv, methods2, originalInterface, className);
    }

    public static MethodMappersAdapter getMethodMappersAdapter(ClassVisitor cv, Class<?> type, String className) {
        Map<Method, java.lang.reflect.Method> methods = getMethodMappers(type);
        return new MethodMappersAdapter(cv, methods, type.getName(), className);
    }

}
