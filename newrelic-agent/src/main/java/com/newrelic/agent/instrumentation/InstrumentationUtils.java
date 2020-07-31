/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

public class InstrumentationUtils {

    public static final int JAVA_7_VERSION_NO = 51;
    private static final int JAVA_CLASS_VERSION_BYTE_OFFSET = 6;

    public static boolean isAbleToResolveAgent(ClassLoader loader, String className) {
        try {
            ClassLoaderCheck.loadAgentClass(loader);
            return true;
        } catch (Throwable t) {
            String msg = MessageFormat.format(
                    "Classloader {0} failed to load Agent class. The agent might need to be loaded by the bootstrap classloader.: {1}",
                    loader.getClass().getName(), t);
            // passing the throwable to the logger can cause deadlock
            // Agent.LOG.log(Level.FINEST, msg, t);
            Agent.LOG.finer(msg);
            return false;
        }
    }

    public static ClassWriter getClassWriter(ClassReader cr, ClassLoader classLoader) {
        int writerFlags = ClassWriter.COMPUTE_MAXS;
        if (shouldComputeFrames(cr)) {
            writerFlags = ClassWriter.COMPUTE_FRAMES;
        }
        return new AgentClassWriter(cr, writerFlags, classLoader);
    }

    private static boolean shouldComputeFrames(ClassReader cr) {
        if (getClassJavaVersion(cr) < JAVA_7_VERSION_NO) {
            return false;
        }
        // Needed for Play classloaders which may throw a RuntimeException in getResourceAsStream().
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig().computeFrames();
    }

    public static byte[] generateClassBytesWithSerialVersionUID(ClassReader classReader, int classReaderFlags,
            ClassLoader classLoader) {
        ClassWriter cw = getClassWriter(classReader, classLoader);
        ClassVisitor cv = new SerialVersionUIDAdder(cw);
        classReader.accept(cv, classReaderFlags);
        return cw.toByteArray();
    }

    public static byte[] generateClassBytesWithSerialVersionUID(byte[] classBytes, int classReaderFlags,
            ClassLoader classLoader) {
        ClassReader cr = new ClassReader(classBytes);
        return generateClassBytesWithSerialVersionUID(cr, classReaderFlags, classLoader);
    }

    public static boolean isAnnotation(ClassReader cr) {
        return (cr.getAccess() & Opcodes.ACC_ANNOTATION) != 0;
    }

    public static boolean isInterface(ClassReader cr) {
        return (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    }

    /**
     * Returns true if the class represented by the class reader supports default methods (Java 8+).
     */
    public static boolean isDefaultMethodSupported(ClassReader cr) {
        return getClassJavaVersion(cr) > JAVA_7_VERSION_NO;
    }

    private static int getClassJavaVersion(ClassReader cr) {
        return cr.readUnsignedShort(JAVA_CLASS_VERSION_BYTE_OFFSET);
    }

    public static Set<Method> getDeclaredMethods(Class<?> clazz) {
        java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
        Set<Method> result = new HashSet<>(methods.length);
        for (java.lang.reflect.Method method : methods) {
            result.add(InstrumentationUtils.getMethod(method));
        }
        return result;
    }

    public static Method getMethod(java.lang.reflect.Method method) {
        Class<?>[] params = method.getParameterTypes();
        Type[] args = new Type[params.length];
        for (int i = 0; i < params.length; i++) {
            args[i] = Type.getType(params[i]);
        }
        return new Method(method.getName(), Type.getType(method.getReturnType()), args);
    }

}
