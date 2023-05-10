/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.google.common.collect.Sets;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Statement;
import java.util.AbstractList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassStructureTest {

    @Test
    public void testClass() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(AbstractList.class);

        assertAbstractList(struct);

        assertClassStructure(AbstractList.class, true);
    }

    @Test
    public void testClassWithNoMethods() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(ExtendsOther.class);
        //jacoco reports manipulate bytecode and inject code, so we need to filter out $jacoco
        Set<Method> filteredMethods = filterJacocoMethods(struct);

        Assert.assertEquals(1, filteredMethods.size());
        Assert.assertTrue(filteredMethods.contains(new Method("<init>", "()V")));

        // assertClassStructure(ExtendsOther.class, true);
    }

    @Test
    public void classAnnotations() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(PrivateMethods.class,
                ClassStructure.METHODS + ClassStructure.CLASS_ANNOTATIONS);

        Assert.assertEquals(1, struct.getClassAnnotations().size());
        assertClassStructure(PrivateMethods.class, true);
    }

    @Test
    public void methodAnnotations() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(PrivateMethods.class,
                ClassStructure.METHOD_ANNOTATIONS);

        Map<String, AnnotationDetails> methodAnnotations = struct.getMethodAnnotations(new Method("getName",
                "()Ljava/lang/String;"));

        Assert.assertNotNull(methodAnnotations);
        Assert.assertEquals(1, methodAnnotations.size());
        AnnotationDetails annotationNode = methodAnnotations.get(Type.getDescriptor(GameMethod.class));
        Assert.assertNotNull(annotationNode);

        Object op = annotationNode.getValue("operationName");
        Assert.assertNotNull(op);
        Assert.assertEquals("CTF", op);

        assertClassStructure(PrivateMethods.class, ClassStructure.METHOD_ANNOTATIONS);
    }

    @Test
    public void testClassWithPrivateMethods() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(PrivateMethods.class);

        Assert.assertTrue(struct.getMethods().contains(new Method("test", "()I")));
        Assert.assertTrue(struct.getMethods().contains(new Method("getName", "()Ljava/lang/String;")));
    }

    @Test
    public void testPrintStream() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(PrintStream.class);

        Assert.assertTrue(struct.getMethods().contains(new Method("println", "(Ljava/lang/String;)V")));
    }

    @Test
    public void testInterface() throws IOException {
        assertClassStructure(Statement.class, false);
    }

    @Test
    public void testEnum() throws IOException {
        assertClassStructure(JmxType.class, false);
    }

    @Test
    public void testAnnotation() throws IOException {
        assertClassStructure(Trace.class, false);
    }

    @Test
    public void testPrivateAnnotation() throws IOException {
        assertClassStructure(PrivateAnnotation.class, false);
    }

    @Test
    public void testProtectedAnnotation() throws IOException {
        assertClassStructure(ProtectedAnnotation.class, false);
    }

    @Test
    public void testObject() throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(Object.class);
        Assert.assertNull(struct.getSuperName());
        assertClassStructure(Object.class, false);
    }

    private void assertClassStructure(Class<?> clazz, boolean checkClassAnnotations) throws IOException {
        int flags = ClassStructure.METHODS;
        if (checkClassAnnotations) {
            flags += ClassStructure.CLASS_ANNOTATIONS;
        }
        assertClassStructure(clazz, flags);
    }

    private void assertClassStructure(Class<?> clazz, int flags) throws IOException {
        ClassStructure struct = ClassStructure.getClassStructure(ClassLoader.getSystemClassLoader().getResource(
                Utils.getClassResourceName(Type.getInternalName(clazz))), flags);
        ClassStructure struct2 = ClassStructure.getClassStructure(clazz, flags);

        assertAccess(struct.getAccess(), struct2.getAccess());
        Assert.assertEquals(struct.getAccess(), struct2.getAccess());
        Assert.assertEquals(struct.getType(), struct2.getType());
        Assert.assertEquals(struct.getSuperName(), struct2.getSuperName());

        Set<Method> methods = new HashSet<>(struct.getMethods());
        // clinit is not returned by Class.getDeclaredConstructors
        methods.remove(new Method("<clinit>", "()V"));
        // not removing jacoco yet because it would change the size for this custom assertion
        if (methods.size() > struct2.getMethods().size()) {
            failMethods(methods, struct2.getMethods());
        } else if (methods.size() > struct2.getMethods().size()) {
            failMethods(methods, struct.getMethods());
        }
        // Jacoco inserts itself into our tests, so we need to filter it out for some assertions
        Set<Method> filteredMethods1 = filterJacocoMethods(struct);
        // clinit is not returned by Class.getDeclaredConstructors
        filteredMethods1.remove(new Method("<clinit>", "()V"));
        Set<Method> filteredMethods2 = filterJacocoMethods(struct2);

        Assert.assertEquals(filteredMethods1, filteredMethods2);
        Assert.assertEquals(Sets.newHashSet(struct.getInterfaces()), Sets.newHashSet(struct2.getInterfaces()));

        if ((flags & ClassStructure.CLASS_ANNOTATIONS) == ClassStructure.CLASS_ANNOTATIONS) {
            Annotation[] annotations = clazz.getAnnotations();

            Assert.assertEquals(annotations.length, struct.getClassAnnotations().size());
            Assert.assertEquals(struct.getClassAnnotations().size(), struct2.getClassAnnotations().size());

            assertAnnotations(struct.getClassAnnotations(), struct2.getClassAnnotations());
        }

        if ((flags & ClassStructure.METHOD_ANNOTATIONS) == ClassStructure.METHOD_ANNOTATIONS) {
            for (Method m : struct.getMethods()) {
                assertAnnotations(struct.getMethodAnnotations(m), struct2.getMethodAnnotations(m));
            }
        }
    }

    private void assertAnnotations(Map<String, AnnotationDetails> methodAnnotations,
            Map<String, AnnotationDetails> methodAnnotations2) {
        Assert.assertNotNull(methodAnnotations);
        Assert.assertNotNull(methodAnnotations2);

        Assert.assertEquals(methodAnnotations.size(), methodAnnotations2.size());

        for (Entry<String, AnnotationDetails> entry : methodAnnotations.entrySet()) {
            AnnotationDetails annotation = methodAnnotations2.get(entry.getKey());
            Assert.assertNotNull(entry.getKey(), annotation);

            Assert.assertEquals(entry.getValue(), annotation);

        }
    }

    private void assertAccess(int access, int access2) {
        if (access != access2) {
            Set<String> modifiers = getModifiers(access);
            Set<String> modifiers2 = getModifiers(access2);

            Assert.assertEquals(modifiers, modifiers2);
            Assert.assertEquals(access, access2);
        }
    }

    private Set<String> getModifiers(int access) {
        Set<String> set = new HashSet<>();
        for (Modifier m : EnumSet.allOf(Modifier.class)) {
            if ((access & m.code) != 0) {
                set.add(m.name());
            }
        }
        return set;
    }

    private void failMethods(Set<Method> methods, Set<Method> methods2) {
        Set<Method> m = new HashSet<>(methods);
        m.removeAll(methods2);
        Assert.fail(m.toString());
    }

    private void assertAbstractList(ClassStructure struct) {
        Assert.assertEquals("java/util/AbstractCollection", struct.getSuperName());
        Assert.assertEquals(1, struct.getInterfaces().length);
        Assert.assertEquals("java/util/List", struct.getInterfaces()[0]);

        int expectedNumMethods = 0;
        String versionSystemProp = System.getProperty("java.specification.version");
        if ("1.6".equals(versionSystemProp)) {
            expectedNumMethods = 17;
        } else if ("1.7".equals(versionSystemProp) || "1.8".equals(versionSystemProp)) {
            expectedNumMethods = 19;
        } else {
            expectedNumMethods = 20;
        }
        Assert.assertEquals(expectedNumMethods, struct.getMethods().size());
    }

    protected static @interface ProtectedAnnotation {
    }

    private static @interface PrivateAnnotation {
    }

    protected enum Modifier implements Opcodes {
        PUBLIC(ACC_PUBLIC),
        PRIVATE(ACC_PRIVATE),
        PROTECTED(ACC_PROTECTED),
        FINAL(ACC_FINAL),
        SUPER(ACC_SUPER),
        INTERFACE(ACC_INTERFACE),
        ABSTRACT(ACC_ABSTRACT),
        SYNTHETIC(ACC_SYNTHETIC),
        ANNOTATION(ACC_ANNOTATION),
        MODULE(ACC_MODULE); // only exists in >= Java 9

        private int code;

        private Modifier(int code) {
            this.code = code;
        }
    }

    private static class ExtendsOther {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface GameService {
        String name() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface GameMethod {
        String operationName() default "";
    }

    @GameService(name = "ScoreService")
    public static class PrivateMethods {

        @GameMethod(operationName = "CTF")
        private String getName() {
            return null;
        }

        int test() {
            return 0;
        }
    }

    // helper method to filter out jacoco injected code.
    private Set<Method> filterJacocoMethods(ClassStructure classStructure) {
        return classStructure.getMethods().stream()
                .filter(method -> !method.toString().contains("$jacoco"))
                .collect(Collectors.toSet());
    }

}
