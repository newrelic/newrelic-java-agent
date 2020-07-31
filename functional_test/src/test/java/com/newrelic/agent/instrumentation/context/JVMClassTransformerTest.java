/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.HashSafeClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Test the jvm class transformer registered by {@link InstrumentationContextManager}
 */
public class JVMClassTransformerTest {
    private final ClassFileTransformer transformer;
    private final ClassLoader classloader = Thread.currentThread().getContextClassLoader();

    @BeforeClass
    public static void init() {
        try {
            Field cacheField = Class.class.getDeclaredField("useCaches");
            cacheField.setAccessible(true);
            cacheField.setBoolean(Class.class, false);
            // System.out.println("reflection cache: " + cacheField.get(TestClass.class));
            cacheField.setAccessible(false);
        } catch (Exception e) {
            // some jvms might not have this field so getting here isn't a big deal
            e.printStackTrace();
        }
    }

    public JVMClassTransformerTest() {
        try {
            this.transformer = ServiceFactory.getClassTransformerService().getContextManager().getJvmClassTransformer();
            // ServiceFactory.getClassTransformerService().getContextManager().getInstrumentation().removeTransformer(transformer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConcurrentTransforms() throws Exception {
        final int numClasses = 300;
        final int concurrencyLevel = 30;

        List<Future<Class<?>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        for (int i = 0; i < numClasses; ++i) {
            final String generatedClassname = TestClass.class.getName() + i;
            Callable<Class<?>> callable = new Callable<Class<?>>() {
                @Override
                public Class<?> call() throws Exception {
                    // make a new class
                    byte[] newClassBytes = cloneClassWithNewName(TestClass.class, generatedClassname);

                    for (String methodname : TestClass.methodsToTrace) {
                        DefaultClassAndMethodMatcher matcher = new HashSafeClassAndMethodMatcher(new ExactClassMatcher(
                                generatedClassname), new ExactMethodMatcher(methodname,
                                Type.getMethodDescriptor(Type.VOID_TYPE)));
                        ServiceFactory.getClassTransformerService().addTraceMatcher(matcher, "FunctionalTest");
                    }
                    // run the new class through the transformer
                    newClassBytes = transformer.transform(classloader, generatedClassname, null,
                            classloader.getClass().getProtectionDomain(), newClassBytes);
                    if (null == newClassBytes) {
                        Assert.fail("Class transformer returned null for class " + generatedClassname);
                    }

                    // load the new class. This will cause the transformer to run again
                    addToContextClassloader(generatedClassname, newClassBytes);
                    Class<?> newClass = classloader.loadClass(generatedClassname);
                    return newClass;
                }
            };
            futures.add(executor.submit(callable));
        }

        List<Class<?>> toRetransform = new ArrayList<>(futures.size());
        for (Future<Class<?>> future : futures) {
            toRetransform.add(future.get());
        }
        ServiceFactory.getClassTransformerService().getContextManager().getInstrumentation().retransformClasses(
                toRetransform.toArray(new Class[0]));

        String uninstrumentedClassMethodNames = "";
        int numUninstrumented = 0;
        for (Future<Class<?>> future : futures) {
            Class<?> clazz = future.get();
            if (null == clazz) {
                numUninstrumented++;
                continue;
            }
            for (String methodname : TestClass.methodsToTrace) {
                if (!isInstrumented(clazz.getMethod(methodname))) {
                    if (numUninstrumented == 0) {
                        uninstrumentedClassMethodNames = clazz.getName() + ":" + methodname;
                    } else {
                        uninstrumentedClassMethodNames += "," + clazz.getName() + ":" + methodname;
                    }
                    numUninstrumented++;
                }
            }
        }
        if (numUninstrumented > 0) {
            Assert.fail(numUninstrumented + " generated methods were not instrumented: "
                    + uninstrumentedClassMethodNames);
        }
        executor.shutdown();
    }

    private boolean isInstrumented(Method method, InstrumentationType... instrumentationTypes) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof InstrumentedMethod) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a clone of the given class.
     * 
     * @param clazz The class to clone.
     * @param newname The clone's name.
     * @return The clone as a byte array.
     * @throws IOException
     */
    private byte[] cloneClassWithNewName(Class clazz, String newname) throws IOException {
        byte[] classBytes = getClassBytesFromClassLoaderResource(clazz.getName(),
                Thread.currentThread().getContextClassLoader());
        ClassNode node = convertToClassNode(classBytes);
        node.name = newname.replace('.', '/');
        return convertToClassBytes(node);
    }

    /**
     * Read a class into an ASM ClassNode represented with its full Java classname, e.g. "java.lang.Object"
     *
     * @param name full Java classname, e.g. "java.lang.Object"
     * @return ASM ClassNode representation of the
     * @throws IOException
     */
    public static ClassNode readClass(String name) throws IOException {
        ClassReader reader = new ClassReader(name);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        reader.accept(result, ClassReader.SKIP_FRAMES);
        return result;
    }

    /**
     * From http://asm.ow2.org/doc/faq.html#Q5
     *
     * @param className class name
     * @param classBytes binary class data
     * @return class instance that was loaded into the classloader
     */
    public static synchronized Class<?> addToContextClassloader(String className, byte[] classBytes) {
        Class clazz = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class cls = Class.forName("java.lang.ClassLoader");
            java.lang.reflect.Method method = cls.getDeclaredMethod("defineClass", String.class, byte[].class,
                    int.class, int.class);

            method.setAccessible(true);
            try {
                Object[] args = new Object[] { className, classBytes, 0, classBytes.length };
                clazz = (Class) method.invoke(loader, args);
            } finally {
                method.setAccessible(false);
            }
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof LinkageError)) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return clazz;
    }

    /**
     * Get bytes representing the specified class name.
     *
     * @param classname class name
     * @return byte[] of the class
     * @throws IOException if class cannot be found/read
     */
    public static byte[] getClassBytes(String classname) throws IOException {
        return getClassBytesFromClassLoaderResource(classname, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Read an array of class bytes into an ASM ClassNode
     *
     * @param classBytes classBytes from a .class file
     * @return ASM ClassNode representation of the
     */
    public static ClassNode convertToClassNode(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        reader.accept(result, ClassReader.SKIP_FRAMES);
        return result;
    }

    /**
     * Converts an ASM {@link ClassNode} to a byte array
     *
     * @param classNode class node to convert
     * @return byte array representing the specified class node
     */
    public static byte[] convertToClassBytes(ClassNode classNode) {
        return convertToClassBytes(classNode, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Converts an ASM {@link ClassNode} to a byte array. Us
     *
     * @param classNode class node to convert
     * @param classloader the classloader used to create the {@link PatchedClassWriter}
     * @return byte array representing the specified class node
     */
    public static byte[] convertToClassBytes(ClassNode classNode, ClassLoader classloader) {
        // ClassWriter cw = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, classloader);
        ClassWriter cw = new ClassWriter(WeaveUtils.ASM_API_LEVEL);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Read a ClassLoader's resource into a byte array.
     *
     * @param classname Internal or Fully qualified name of the class
     * @param classloader the classloader to read the resource from
     * @return the resource bytes (class bytes) or null if no resource was found.
     * @throws IOException
     */
    public static byte[] getClassBytesFromClassLoaderResource(String classname, ClassLoader classloader)
            throws IOException {
        InputStream is = classloader.getResourceAsStream(classname.replace('.', '/') + ".class");
        if (null == is) {
            return null; // no such resource
        }

        return Streams.read(is, true);
    }

}
