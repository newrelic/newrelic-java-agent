/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * The normal ClassWriter uses the context classloader to resolve classes as it tries to compute frames which doesn't
 * work for us because we're usually using alternate classloaders and referencing classes that can't be resolved through
 * normal methods (they may not even be loaded yet). Also, the base ClassWriter has a side effect of causing classes to
 * load which will cause trouble.
 * 
 * This writer overrides the methods that try to load classes and uses a {@link ClassResolver} to understand the class
 * structure without actually loading any classes.
 */
public class PatchedClassWriter extends ClassWriter {
    static final String JAVA_LANG_OBJECT = "java/lang/Object";
    protected final ClassResolver classResolver;

    public PatchedClassWriter(int flags, ClassLoader classLoader) {
        this(flags, ClassResolvers.getClassLoaderResolver(classLoader == null ? AgentBridge.getAgent().getClass().getClassLoader()
                : classLoader));
    }

    public PatchedClassWriter(int flags, ClassResolver classResolver) {
        super(flags);

        this.classResolver = classResolver;
    }

    // @Override
    // protected String getCommonSuperClass(final String type1, final String type2) {
    // Class<?> c, d;
    // try {
    // c = Class.forName(type1.replace('/', '.'), false, classLoader);
    // d = Class.forName(type2.replace('/', '.'), false, classLoader);
    // } catch (Exception e) {
    // throw new RuntimeException(e.toString());
    // }
    // if (c.isAssignableFrom(d)) {
    // return type1;
    // }
    // if (d.isAssignableFrom(c)) {
    // return type2;
    // }
    // if (c.isInterface() || d.isInterface()) {
    // return "java/lang/Object";
    // } else {
    // do {
    // c = c.getSuperclass();
    // } while (!c.isAssignableFrom(d));
    // return c.getName().replace('.', '/');
    // }
    // }

    // @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        if (JAVA_LANG_OBJECT.equals(type1) || JAVA_LANG_OBJECT.equals(type2)) {
            return JAVA_LANG_OBJECT;
        }

        try {
            ClassReader reader1 = getClassReader(type1);
            ClassReader reader2 = getClassReader(type2);

            if (reader1 == null || reader2 == null) {
                return JAVA_LANG_OBJECT;
            }

            String superClass = getCommonSuperClass(reader1, reader2);

            if (superClass == null) {
                // System.err.println(type1 + "?" + type2 + "  |  Super: " + superClass);
                return JAVA_LANG_OBJECT;
            }
            return superClass;
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINER, ex, "Unable to get common super class");
            throw new RuntimeException(ex.toString());
        }
    }

    protected ClassReader getClassReader(String type) throws IOException {

        try (InputStream classResource = classResolver.getClassResource(type)) {
            return classResource == null ? null : new ClassReader(classResource);
        } catch (IOException ex) {
            Agent.LOG.log(Level.FINEST, ex.toString(), ex);
            return null;
        }
    }

    private String getCommonSuperClass(ClassReader reader1, ClassReader reader2) throws ClassNotFoundException,
            IOException {
        if (isAssignableFrom(reader1, reader2)) {
            return reader1.getClassName();
        }
        if (isAssignableFrom(reader2, reader1)) {
            return reader2.getClassName();
        }

        if (isInterface(reader1) || isInterface(reader2)) {
            return JAVA_LANG_OBJECT;
        }

        Set<String> classes = new HashSet<>();
        classes.add(reader1.getClassName());
        while (reader1.getSuperName() != null) {
            classes.add(reader1.getSuperName());
            reader1 = getClassReader(reader1.getSuperName());
        }

        while (reader2.getSuperName() != null) {
            if (classes.contains(reader2.getClassName())) {
                return reader2.getClassName();
            }
            reader2 = getClassReader(reader2.getSuperName());
        }

        return null; // "java/lang/Object";
    }

    private boolean isInterface(ClassReader reader) {
        return (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    }

    private boolean isAssignableFrom(ClassReader reader1, ClassReader reader2) {
        // if (JAVA_LANG_OBJECT.equals(reader1.getClassName()) || JAVA_LANG_OBJECT.equals(reader2.getClassName())) {
        // return JAVA_LANG_OBJECT;
        // }
        return reader1.getClassName().equals(reader2.getClassName())
                || reader1.getClassName().equals(reader2.getSuperName());
    }
}
