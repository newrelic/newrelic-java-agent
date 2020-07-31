/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.google.common.collect.ImmutableSet;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.Weaver;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

public class WeaveWithAnnotationClassMatchTest {
    @BeforeClass
    public static void beforeClass() throws Exception {

        final String annotedClass = "com.newrelic.weave.WeaveWithAnnotationClassMatchTest$AnnotatedClass";
        final String annotedClassWeave = "com.newrelic.weave.WeaveWithAnnotationClassMatchTest$AnnotedClass_Weave";
        WeaveTestUtils.weaveAnnotedTypeAndAddToContextClassloader(annotedClass, annotedClassWeave,
                ImmutableSet.of("com.newrelic.weave.WeaveWithAnnotationClassMatchTest$CustomAnnotation"),
                Collections.<String>emptySet());

        final String protectedAnnotedClass = "com.newrelic.weave.WeaveWithAnnotationClassMatchTest$ProtectedAnnotatedClass";
        final String protectedAnnotedClassWeave = "com.newrelic.weave.WeaveWithAnnotationClassMatchTest$ProtectedAnnotatedClass_Weave";
        WeaveTestUtils.weaveAnnotedTypeAndAddToContextClassloader(protectedAnnotedClass, protectedAnnotedClassWeave,
                ImmutableSet.of("com.newrelic.weave.WeaveWithAnnotationClassMatchTest$CustomAnnotation"),
                Collections.<String>emptySet());

        final String privateAnnotatedClass = "com.newrelic.weave.WeaveWithAnnotationClassMatchTest$PrivateAnnotatedClass";
        final String privateAnnotatedClassWeave = "com.newrelic.weave.WeaveWithAnnotationClassMatchTest$PrivateAnnotatedClass_Weave";
        WeaveTestUtils.weaveAnnotedTypeAndAddToContextClassloader(privateAnnotatedClass, privateAnnotatedClassWeave,
                ImmutableSet.of("com.newrelic.weave.WeaveWithAnnotationClassMatchTest$CustomAnnotation"),
                Collections.<String>emptySet());
    }

    @interface CustomAnnotation {}

    @CustomAnnotation
    static class AnnotatedClass {
        public static int count;

        public int getCount() { return count;}
    }

    public static class AnnotedClass_Weave {
        public static int count = Weaver.callOriginal();

       @WeaveIntoAllMethods
        private static void instrumentation() {
           count++;
       }
    }

    @CustomAnnotation
    protected static class ProtectedAnnotatedClass {
        public static int count;

        public int getCount() { return count;}
    }

    static class ProtectedAnnotatedClass_Weave {
        public static int count = Weaver.callOriginal();

        @WeaveIntoAllMethods
        private static void instrumentation() {
            count++;
        }
    }


    @CustomAnnotation
    private static class PrivateAnnotatedClass {
        public static int count;

        public int getCount() { return count;}
    }

    static class PrivateAnnotatedClass_Weave {
        public static int count = Weaver.callOriginal();

        @WeaveIntoAllMethods
        private static void instrumentation() {
            count++;
        }
    }

    @Test
    public void testNoAccessMismatch() {
        AnnotatedClass annotatedClass = new AnnotatedClass();
        annotatedClass.getCount();
        annotatedClass.getCount();
        annotatedClass.getCount();
        assertEquals(3, AnnotatedClass.count);

        ProtectedAnnotatedClass protectedClass = new ProtectedAnnotatedClass();
        protectedClass.getCount();
        protectedClass.getCount();
        protectedClass.getCount();
        assertEquals(3, ProtectedAnnotatedClass.count);

        PrivateAnnotatedClass privateAnnotatedClass = new PrivateAnnotatedClass();
        privateAnnotatedClass.getCount();
        privateAnnotatedClass.getCount();
        privateAnnotatedClass.getCount();
        assertEquals(3, PrivateAnnotatedClass.count);
    }

}
