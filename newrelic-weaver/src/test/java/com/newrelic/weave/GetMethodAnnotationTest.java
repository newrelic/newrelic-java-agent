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
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;

public class GetMethodAnnotationTest {
    @BeforeClass
    public static void beforeClass() throws IOException {
        final String classMethodNoAnnotation = "com.newrelic.weave.GetMethodAnnotationTest$ClassMethodNoAnnotation";
        final String classMethodNoAnnotationWeave = "com.newrelic.weave.GetMethodAnnotationTest$ClassMethodNoAnnotation_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(classMethodNoAnnotation, classMethodNoAnnotationWeave);

        final String classMethodAnnotation = "com.newrelic.weave.GetMethodAnnotationTest$ClassMethodWithAnnotation";
        final String classMethodAnnotationWeave = "com.newrelic.weave.GetMethodAnnotationTest$ClassMethodWithAnnotation_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(classMethodAnnotation, classMethodAnnotationWeave);

        final String classMethodAnnotationStaticMethod = "com.newrelic.weave.GetMethodAnnotationTest$ClassMethodWithAnnotationStatic";
        final String classMethodAnnotationStaticMethodWeave = "com.newrelic.weave.GetMethodAnnotationTest$ClassMethodWithAnnotationStatic_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(classMethodAnnotationStaticMethod, classMethodAnnotationStaticMethodWeave);
    }

    @Target(ElementType.METHOD)
    @interface MethodAnnotation {
        int number();
    }

    static class ClassMethodNoAnnotation {

        public int methodNoAnnotation() {
            return 0;
        }

    }

    static class ClassMethodNoAnnotation_Weave {

        public int methodNoAnnotation() {
            MethodAnnotation methodAnnotation = Weaver.getMethodAnnotation(MethodAnnotation.class);
            if (methodAnnotation == null) {
                return Weaver.callOriginal();
            } else {
                return methodAnnotation.number();
            }
        }

    }


    static class ClassMethodWithAnnotation {

        @MethodAnnotation(number = 42)
        public int methodNoAnnotation() {
            return 0;
        }

    }

    static class ClassMethodWithAnnotation_Weave {

        public int methodNoAnnotation() {
            MethodAnnotation methodAnnotation = Weaver.getMethodAnnotation(MethodAnnotation.class);
            if (methodAnnotation == null) {
                return Weaver.callOriginal();
            } else {
                return methodAnnotation.number();
            }
        }
    }


    static class ClassMethodWithAnnotationStatic {

        @MethodAnnotation(number = 42)
        public static int methodNoAnnotation() {
            return 0;
        }

    }

    static class ClassMethodWithAnnotationStatic_Weave {

        public static int methodNoAnnotation() {
            MethodAnnotation methodAnnotation = Weaver.getMethodAnnotation(MethodAnnotation.class);
            if (methodAnnotation == null) {
                return Weaver.callOriginal();
            } else {
                return methodAnnotation.number();
            }
        }

    }

    @Test
    public void testNoMethodAnnotation() {
        assertEquals(0, new ClassMethodNoAnnotation().methodNoAnnotation());
    }

    @Test
    public void testMethodWithAnnotation() {
        assertEquals(42, new ClassMethodWithAnnotation().methodNoAnnotation());
    }

    @Test
    public void testStaticMethodWithAnnotation() {
        assertEquals(42, new ClassMethodWithAnnotationStatic().methodNoAnnotation());
    }
}

