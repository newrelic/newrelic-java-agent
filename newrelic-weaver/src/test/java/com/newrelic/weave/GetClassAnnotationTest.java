/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.api.agent.weaver.Weaver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class GetClassAnnotationTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        final String original = "com.newrelic.weave.GetClassAnnotationTest$AnnotatedClass";
        final String weave = "com.newrelic.weave.GetClassAnnotationTest$AnnotedClass_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(original, weave);

        final String noAnnotations = "com.newrelic.weave.GetClassAnnotationTest$ClassWithoutAnnotations";
        WeaveTestUtils.weaveAndAddToContextClassloader(noAnnotations, weave);

        final String staticMethodClass = "com.newrelic.weave.GetClassAnnotationTest$StaticMethodsClass";
        final String staticMethodWeave = "com.newrelic.weave.GetClassAnnotationTest$StaticMethodClass_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(staticMethodClass, staticMethodWeave);
    }

    static @interface ClassAnnotation {
        String name();
        int number();
    }

    @ClassAnnotation(name = "foo", number = 42)
    static class AnnotatedClass {

        public String getName() {
            return null;
        }

        public int getNumber() {
            return 0;
        }
    }

    static class AnnotedClass_Weave {
        public String getName() {
            ClassAnnotation classAnnotation = Weaver.getClassAnnotation(ClassAnnotation.class);
            if (classAnnotation == null) {
                return Weaver.callOriginal();
            } else {
                return classAnnotation.name();
            }
        }

        public int getNumber() {
            ClassAnnotation classAnnotation = Weaver.getClassAnnotation(ClassAnnotation.class);
            if (classAnnotation == null) {
                return Weaver.callOriginal();
            } else {
                return classAnnotation.number();
            }
        }
    }

    @ClassAnnotation(name = "bar", number = 7)
    static class StaticMethodsClass {

        public static int getNumber() {
            return 0;
        }
    }

    static class StaticMethodClass_Weave {
        public static int getNumber() {
            ClassAnnotation classAnnotation = Weaver.getClassAnnotation(ClassAnnotation.class);
            if (classAnnotation == null) {
                return Weaver.callOriginal();
            } else {
                return classAnnotation.number();
            }
        }
    }

    static class ClassWithoutAnnotations {

        public String getName() {
            return null;
        }

        public int getNumber() {
            return 0;
        }
    }


    @Test
    public void testGetClassAnnotation() {
        AnnotatedClass annotatedClass = new AnnotatedClass();
        assertEquals("foo", annotatedClass.getName());
        assertEquals(42, annotatedClass.getNumber());
    }

    @Test
    public void testGetClassAnnotationNoAnnotations() {
        ClassWithoutAnnotations classWithoutAnnotations = new ClassWithoutAnnotations();
        assertEquals(null, classWithoutAnnotations.getName());
        assertEquals(0, classWithoutAnnotations.getNumber());
    }

    @Test
    public void testGetClassAnnotationStaticMethod() {
        StaticMethodsClass classWithoutAnnotations = new StaticMethodsClass();
        assertEquals(7, classWithoutAnnotations.getNumber());
    }

}
