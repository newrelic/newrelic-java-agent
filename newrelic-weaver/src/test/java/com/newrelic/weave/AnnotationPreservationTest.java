/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that annoatations on types, fields, methods, construcotrs, and parameters are preserved as appropriate during
 * weaving. New fields, new methods, and interface-matched constructors, will not have annotations preserved.
 */
public class AnnotationPreservationTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.AnnotationPreservationTest$BothTarget",
                "com.newrelic.weave.AnnotationPreservationTest$BothWeave");
        assertNull(weave.getMatch().getExtension());

        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.AnnotationPreservationTest$SingleTarget",
                "com.newrelic.weave.AnnotationPreservationTest$SingleWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.AnnotationPreservationTest$DoubleTarget",
                "com.newrelic.weave.AnnotationPreservationTest$DoubleWeave");
    }

    public static void assertAnnotations(AnnotatedElement actual, Class<?>... expected) {
        assertAnnotations(actual.getAnnotations(), expected);
    }

    public static void assertAnnotations(Annotation[] actual, Class<?>... expected) {
        String message = String.format("\n\tExpected: %s\n\tActual %s", Arrays.toString(expected),
                Arrays.toString(actual));

        Set<Class<?>> actualTypes = new HashSet<>();
        for (Annotation annotation : actual) {
            actualTypes.add(annotation.annotationType());
        }

        assertEquals(message, actual.length, expected.length);
        for (Class<?> expectedType : expected) {
            assertTrue(message, actualTypes.contains(expectedType));
        }
    }

    @Test
    public void testClassAnnotations() {
        assertAnnotations(BothTarget.class, TargetAnnotation.class, WeaveAnnotation.class);
        assertAnnotations(SingleTarget.class, WeaveAnnotation.class);
        assertAnnotations(DoubleTarget.class, TargetAnnotation.class);
        assertEquals("weave", DoubleTarget.class.getAnnotation(TargetAnnotation.class).value());
    }

    @Test
    public void testConstructorAnnotations() throws NoSuchMethodException {
        assertAnnotations(BothTarget.class.getConstructors()[0], TargetAnnotation.class, WeaveAnnotation.class);
        assertAnnotations(SingleTarget.class.getConstructors()[0], WeaveAnnotation.class);
        assertAnnotations(DoubleTarget.class.getConstructors()[0], TargetAnnotation.class);
        assertEquals("weave", DoubleTarget.class.getConstructors()[0].getAnnotation(TargetAnnotation.class).value());
    }

    @Test
    public void testFieldAnnotations() throws NoSuchFieldException {
        assertAnnotations(BothTarget.class.getField("bothField"), TargetAnnotation.class, WeaveAnnotation.class);
        assertAnnotations(BothTarget.class.getField("targetField"), TargetAnnotation.class);
        assertAnnotations(BothTarget.class.getField("weaveField"), WeaveAnnotation.class);
        assertAnnotations(BothTarget.class.getField("doubleField"), TargetAnnotation.class);
        assertEquals("weave", BothTarget.class.getField("doubleField").getAnnotation(TargetAnnotation.class).value());
    }

    @Test
    public void testMethodAnnotations() throws NoSuchMethodException {
        assertAnnotations(BothTarget.class.getMethod("bothMethod"), TargetAnnotation.class, WeaveAnnotation.class);
        assertAnnotations(BothTarget.class.getMethod("targetMethod"), TargetAnnotation.class);
        assertAnnotations(BothTarget.class.getMethod("weaveMethod"), WeaveAnnotation.class);
        assertAnnotations(BothTarget.class.getMethod("doubleMethod"), TargetAnnotation.class);
        assertEquals("weave", BothTarget.class.getMethod("doubleMethod").getAnnotation(TargetAnnotation.class).value());
    }

    @Test
    public void testParameterAnnotations() throws NoSuchMethodException {
        Method m = BothTarget.class.getMethod("params", String.class, String.class, String.class, String.class);
        Annotation[][] parameterAnnotations = m.getParameterAnnotations();
        assertAnnotations(parameterAnnotations[0], TargetAnnotation.class, WeaveAnnotation.class);
        assertAnnotations(parameterAnnotations[1], TargetAnnotation.class);
        assertAnnotations(parameterAnnotations[2], WeaveAnnotation.class);
        assertAnnotations(parameterAnnotations[3], TargetAnnotation.class);
        assertEquals("weave", ((TargetAnnotation) parameterAnnotations[3][0]).value());
    }

    // some annotations
    @Target({ TYPE, FIELD, METHOD, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    public @interface TargetAnnotation {
        String value() default "";
    }

    @Target({ TYPE, FIELD, METHOD, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    public @interface WeaveAnnotation {
    }

    // some classes
    @TargetAnnotation
    public static class BothTarget {

        @TargetAnnotation
        public BothTarget(int i) {
        }

        @TargetAnnotation
        public String bothField; // annotated by both target and weave

        @TargetAnnotation
        public String targetField; // annotated by target only

        public String weaveField; // annotated by weave only

        @TargetAnnotation
        public String doubleField; // has @Target on both weave and original!

        @TargetAnnotation
        public void bothMethod() {
        } // annotated by both target and weave

        @TargetAnnotation
        public void targetMethod() {
        } // annotated by target only

        public void weaveMethod() {
        } // annotated by weave only

        @TargetAnnotation
        public void doubleMethod() {
        } // has @TargetAnnotation on both weave and original!

        public void params(@TargetAnnotation String bothParam, @TargetAnnotation String targetParam, String weaveParam,
                @TargetAnnotation String doubleParam) {
        }
    }

    @WeaveAnnotation
    public static class BothWeave {

        @WeaveAnnotation
        public BothWeave(int i) {
        }

        @WeaveAnnotation
        public String bothField;

        public String targetField;

        @WeaveAnnotation
        public String weaveField;

        @TargetAnnotation("weave")
        public String doubleField;

        @WeaveAnnotation
        public void bothMethod() {
        }

        public void targetMethod() {
        }

        @WeaveAnnotation
        public void weaveMethod() {
        }

        @TargetAnnotation("weave")
        public void doubleMethod() {
        }

        public void params(@WeaveAnnotation String bothParam, String targetParam, @WeaveAnnotation String weaveParam,
                @TargetAnnotation("weave") String doubleParam) {
        }
    }

    public static class SingleTarget {

    }

    @WeaveAnnotation
    public static class SingleWeave {

        @WeaveAnnotation
        public SingleWeave() {
        }
    }

    @TargetAnnotation
    public static class DoubleTarget {

        @TargetAnnotation
        public DoubleTarget() {
        }

    }

    @TargetAnnotation(value = "weave")
    public static class DoubleWeave {

        @TargetAnnotation(value = "weave")
        public DoubleWeave() {
        }

    }
}
