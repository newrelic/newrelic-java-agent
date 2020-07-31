/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.util.Annotations;
import com.newrelic.agent.util.Strings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InterfaceMixinClassTransformer extends AbstractClassTransformer {

    private final Map<String, List<Class<?>>> interfaceVisitors = new HashMap<>();

    public InterfaceMixinClassTransformer(int classreaderFlags) {
        super(classreaderFlags);
    }

    @Override
    protected void start() {
        addInterfaceMixins();
    }

    private void addInterfaceMixins() {
        Collection<Class<?>> classes = new Annotations().getInterfaceMixinClasses();
        for (Class<?> clazz : classes) {
            addInterfaceMixin(clazz);
        }
    }

    /**
     * For testing.
     */
    protected void addInterfaceMixin(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        InterfaceMixin mixin = clazz.getAnnotation(InterfaceMixin.class);
        if (mixin == null) {
            // This test is superficially bizarre, since the class was originally
            // selected because of the presence of the InterfaceMixin annotation.
            // So why would it be unavailable now? The answer lies in the mysteries
            // of Java class loader delegation. If you see the following message in
            // a log, begin by reading the code in BootstrapLoader.java which tries
            // to force all classes annotated with @InterfaceMixin, plus the
            // annotation class itself, on to the bootstrap classloader by dynamically
            // generating a jar file during the premain.
            Agent.LOG.log(Level.FINER, "InterfaceMixin access failed: " + clazz.getName());
            return;
        }
        for (String className : mixin.originalClassName()) {
            String key = Strings.fixInternalClassName(className);
            List<Class<?>> value = interfaceVisitors.get(key);
            if (value == null) {
                value = new ArrayList<>(1);
            }
            value.add(clazz);
            interfaceVisitors.put(key, value);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (!PointCutClassTransformer.isValidClassName(className)) {
                return null;
            }
            if (!matches(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)) {
                return null;
            }
            if (!isAbleToResolveAgent(loader, className)) {
                return null;
            }
            List<Class<?>> clazzes = interfaceVisitors.get(className);
            if (clazzes == null || clazzes.size() == 0) {
                return null;
            }

            // Not adding UID. See JAVA-3380
            return transform(classfileBuffer, clazzes, loader, className);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, t, "Instrumentation error for {0}", className);
            return null;
        }
    }

    private byte[] transform(byte[] classBytesWithUID, List<Class<?>> clazzes, ClassLoader loader, String className)
            throws Exception {
        byte[] classBytes = classBytesWithUID;
        byte[] oldClassBytes = classBytesWithUID;
        for (Class<?> clazz : clazzes) {
            try {
                classBytes = transform(classBytes, clazz, loader, className);
                oldClassBytes = classBytes;
            } catch (StopProcessingException e) {
                String msg = MessageFormat.format("Failed to append {0} to {1}: {2}", clazz.getName(), className, e);
                Agent.LOG.fine(msg);
                classBytes = oldClassBytes;
            }
        }
        final String msg = MessageFormat.format("Instrumenting {0}", className);
        Agent.LOG.finer(msg);
        return classBytes;
    }

    private byte[] transform(byte[] classBytes, Class<?> clazz, ClassLoader loader, String className) throws Exception {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = InstrumentationUtils.getClassWriter(cr, loader);
        ClassVisitor classVisitor = getClassVisitor(cw, className, clazz, loader);
        cr.accept(classVisitor, getClassReaderFlags());
        Agent.LOG.log(Level.FINEST, "InterfaceMixingClassTransformer.transform(bytes, clazz, {0}, {1})", loader,
                className);
        return cw.toByteArray();
    }

    private ClassVisitor getClassVisitor(ClassVisitor classVisitor, String className, Class<?> clazz, ClassLoader loader) {
        ClassVisitor adapter = new AddInterfaceAdapter(classVisitor, className, clazz);
        adapter = RequireMethodsAdapter.getRequireMethodsAdaptor(adapter, className, clazz, loader);
        adapter = new FieldAccessorGeneratingClassAdapter(adapter, className, clazz);
        return adapter;
    }

    @Override
    protected boolean isRetransformSupported() {
        return false;
    }

    @Override
    protected boolean matches(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return interfaceVisitors.containsKey(className);
    }

    @Override
    protected ClassVisitor getClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader) {
        return null;
    }

}
