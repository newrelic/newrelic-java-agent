/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;

public class Utils {

    private static final String PROXY_CLASS_NAME = "java/lang/reflect/Proxy";

    private Utils() {
    }

    /**
     * This is here for testing. Generally you should already have a classreader and therefore can call the other
     * isProxy method.
     *
     * @param classBytes The bytes for the class.
     * @return True if the class is a proxy, else false.
     */
    public static boolean isJdkProxy(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        return isJdkProxy(reader);
    }

    /**
     * Returns true if the input class bytes are those of a proxy.
     *
     * @param reader Reader with the bytes.
     * @return True if the input bytes are from a proxy class, else false.
     */
    public static boolean isJdkProxy(ClassReader reader) {
        if (reader != null && looksLikeAProxy(reader)) {
            ProxyClassVisitor cv = new ProxyClassVisitor();
            reader.accept(cv, ClassReader.SKIP_CODE);
            return cv.isProxy();
        }
        return false;
    }

    private static boolean looksLikeAProxy(ClassReader reader) {
        return (PROXY_CLASS_NAME.equals(reader.getSuperName()) && Modifier.isFinal(reader.getAccess()));
    }

    /**
     * Returns a ClassReader for the given class by trying to load the bytes of the class through its classloader.
     */
    public static ClassReader readClass(Class<?> theClass) throws IOException, BenignClassReadException {
        if (theClass.isArray()) {
            // We can't call ClassLoader.getResource() for an array (like [com/test/MyClass) - it will return null.
            throw new BenignClassReadException(theClass.getName() + " is an array");
        } else if (Proxy.isProxyClass(theClass)) {
            // we have no way to get to the bytecode that generated a Proxy class
            throw new BenignClassReadException(theClass.getName() + " is a Proxy class");
        } else if (isRMIStubOrProxy(theClass)) {
            throw new BenignClassReadException(theClass.getName() + " is an RMI Stub or Proxy class");
        } else if (theClass.getName().startsWith("sun.reflect.")) {
            // a bunch of generated classes are in this package. They're not marked with an annotation or interface that
            // we can identify them with.
            throw new BenignClassReadException(theClass.getName() + " is a reflection class");
        } else if (theClass.getProtectionDomain().getCodeSource() != null
                && theClass.getProtectionDomain().getCodeSource().getLocation() == null) {
            // Classes missing sourcecode location are likely runtime generated classes.
            // This also applies to our non-instrumentation weave classes.
            throw new BenignClassReadException(theClass.getName() + " is a generated class");
        }
        URL resource = getClassResource(theClass.getClassLoader(), Type.getInternalName(theClass));
        return getClassReaderFromResource(theClass.getName(), resource);
    }

    private static final Set<String> RMI_SUPERCLASSES = ImmutableSet.of(
            "org.omg.stub.javax.management.remote.rmi._RMIConnection_Stub", "com.sun.jmx.remote.internal.ProxyRef");

    private static boolean isRMIStubOrProxy(Class<?> theClass) {
        if (theClass.getSuperclass() == null) {
            return false;
        }
        return RMI_SUPERCLASSES.contains(theClass.getSuperclass().getName());
    }

    public static ClassReader readClass(ClassLoader loader, String internalClassName) throws IOException {
        URL resource = getClassResource(loader, internalClassName);
        return getClassReaderFromResource(internalClassName, resource);
    }

    public static ClassReader getClassReaderFromResource(String internalClassName, URL resource) throws IOException {
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                return new ClassReader(stream);
            }
        } else {
            throw new MissingResourceException("Unable to get the resource stream for class " + internalClassName);
        }
    }

    public static String getClassResourceName(String internalName) {
        return internalName + ".class";
    }

    public static String getClassResourceName(Class<?> clazz) {
        return getClassResourceName(Type.getInternalName(clazz));
    }

    public static URL getClassResource(ClassLoader loader, Type type) {
        return getClassResource(loader, type.getInternalName());
    }

    public static URL getClassResource(ClassLoader loader, String internalClassName) {
        if (loader == null) {
            loader = AgentBridge.getAgent().getClass().getClassLoader();
        }
        if (Agent.LOG.isFinestEnabled() && internalClassName.endsWith(".class.class")) {
            Agent.LOG.finest("Invalid resource name " + internalClassName);
        }
        URL url = loader.getResource(getClassResourceName(internalClassName));
        if (url == null) {
            url = BootstrapLoader.get().findResource(internalClassName);
        }
        return url;
    }

    public static void print(byte[] bytes) {
        print(bytes, new PrintWriter(System.out, true));
    }

    public static String asString(MethodNode method) {
        Printer printer = new Textifier();
        org.objectweb.asm.util.TraceMethodVisitor tv = new org.objectweb.asm.util.TraceMethodVisitor(printer);
        method.accept(tv);
        return Joiner.on(' ').join(printer.getText());
    }

    public static void print(byte[] bytes, PrintWriter pw) {
        ClassReader cr = new ClassReader(bytes);
        org.objectweb.asm.util.TraceClassVisitor mv = new org.objectweb.asm.util.TraceClassVisitor(pw);
        cr.accept(mv, ClassReader.EXPAND_FRAMES);
        pw.flush();
    }

    private static final Set<String> PRIMITIVE_TYPES = ImmutableSet.of(Type.BOOLEAN_TYPE.getClassName(),
            Type.BYTE_TYPE.getClassName(), Type.CHAR_TYPE.getClassName(), Type.DOUBLE_TYPE.getClassName(),
            Type.FLOAT_TYPE.getClassName(), Type.INT_TYPE.getClassName(), Type.LONG_TYPE.getClassName(),
            Type.SHORT_TYPE.getClassName(), Type.VOID_TYPE.getClassName());

    /**
     * Returns true if the given type is a java primitive.
     */
    public static boolean isPrimitiveType(String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    /**
     * Returns the index of the first local variable of a method after the "special" local variables like 'this' and the
     * method arguments.
     */
    public static int getFirstLocal(int access, Method method) {
        Type[] argumentTypes = method.getArgumentTypes();
        int nextLocal = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
        for (int i = 0; i < argumentTypes.length; i++) {
            nextLocal += argumentTypes[i].getSize();
        }
        return nextLocal;
    }

    public static Predicate<Class> getAnnotationsMatcher(AnnotationMatcher annotationMatcher) {
        return clazz -> {
            if (clazz.getClassLoader() == null || clazz.isArray() || clazz.isAnnotation() || clazz.isEnum() ||
                    clazz.isInterface() || Proxy.isProxyClass(clazz)) {
                return false;
            }
            final AtomicBoolean containsTracer = new AtomicBoolean(false);
            final String classResource = clazz.getName().replace('.', '/') + ".class";
            try (InputStream in = clazz.getClassLoader().getResourceAsStream(classResource)) {
                if (in == null) {
                    return false;
                }
                ClassReader classReader = new ClassReader(in);
                classReader.accept(new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                if (!containsTracer.get() && annotationMatcher.matches(descriptor)) {
                                    containsTracer.set(true);
                                }
                                return null;
                            }
                        };
                    }

                }, ClassReader.SKIP_CODE);
            } catch (IOException e) {
                Agent.LOG.log(Level.FINE, e.getMessage(), e);
            }
            return containsTracer.get();
        };
    }
}
