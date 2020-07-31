/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Visit a class or interface to verify it declares required methods.
 */
public class RequireMethodsAdapter extends ClassVisitor {

    private final Set<Method> requiredMethods;
    private final ClassLoader classLoader;
    private final String className;
    private final String requiredInterface;
    private final ClassVisitor missingMethodsVisitor = new MissingMethodsVisitor();

    /**
     * This class verifies that required methods of an interface are declared in the class or its superclasses (or
     * superinterfaces).
     * 
     * @param cv the class visitor delegate
     * @param requiredMethods the required methods
     * @param className the name of the class
     * @param requiredInterface the interface of the required methods
     * @param loader its class loader
     * @throws StopProcessingException if not all required methods are found
     */
    private RequireMethodsAdapter(ClassVisitor cv, Set<Method> requiredMethods, String requiredInterface,
            String className, ClassLoader loader) {
        super(WeaveUtils.ASM_API_LEVEL, cv);
        this.className = className;
        this.requiredInterface = requiredInterface;
        this.classLoader = loader;
        this.requiredMethods = new HashSet<>(requiredMethods);
    }

    public static RequireMethodsAdapter getRequireMethodsAdaptor(ClassVisitor cv, String className, Class<?> type,
            ClassLoader loader) {
        Set<Method> requiredMethods = InstrumentationUtils.getDeclaredMethods(type);
        return new RequireMethodsAdapter(cv, requiredMethods, type.getName(), className, loader);
    }

    public static RequireMethodsAdapter getRequireMethodsAdaptor(ClassVisitor cv, Set<Method> requiredMethods,
            String className, String requiredInterface, ClassLoader loader) {
        return new RequireMethodsAdapter(cv, requiredMethods, requiredInterface, className, loader);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        requiredMethods.remove(new Method(name, desc));
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (requiredMethods.size() > 0) {
            visitSuperclassesOrInterfaces();
        }
        if (requiredMethods.size() > 0) {
            String msg = MessageFormat.format("{0} does not implement these methods: {1} declared in {2}", className,
                    requiredMethods, requiredInterface);
            throw new StopProcessingException(msg);
        }
        super.visitEnd();
    }

    private void visitSuperclassesOrInterfaces() {
        ClassMetadata metadata = new ClassMetadata(className, classLoader);
        if (metadata.isInterface()) {
            visitInterfaces(metadata);
        } else {
            visitSuperclasses(metadata);
        }
    }

    private void visitSuperclasses(ClassMetadata metadata) {
        ClassMetadata superClassMetadata = metadata.getSuperclass();
        while (superClassMetadata != null) {
            ClassReader cr = superClassMetadata.getClassReader();
            cr.accept(missingMethodsVisitor, 0);
            if (requiredMethods.size() == 0) {
                return;
            }
            superClassMetadata = superClassMetadata.getSuperclass();
        }
    }

    private void visitInterfaces(ClassMetadata metadata) {
        Queue<String> pendingInterfaces = new LinkedList<>();
        pendingInterfaces.addAll(Arrays.asList(metadata.getInterfaceNames()));
        String interfaceName = pendingInterfaces.poll();
        while (interfaceName != null) {
            ClassMetadata interfaceMetadata = new ClassMetadata(interfaceName, classLoader);
            ClassReader cr = interfaceMetadata.getClassReader();
            cr.accept(missingMethodsVisitor, 0);
            if (requiredMethods.size() == 0) {
                return;
            }
            pendingInterfaces.addAll(Arrays.asList(interfaceMetadata.getInterfaceNames()));
            interfaceName = pendingInterfaces.poll();
        }
    }

    private class MissingMethodsVisitor extends ClassVisitor {

        private MissingMethodsVisitor() {
            super(WeaveUtils.ASM_API_LEVEL);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            requiredMethods.remove(new Method(name, desc));
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

    }
}
