/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static com.newrelic.weave.violation.WeaveViolationType.CLASS_MISSING_REQUIRED_ANNOTATIONS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Type;

import com.google.common.collect.Sets;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.violation.WeaveViolation;

public class WeaveAnnotatedTypeTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String originalClass = "com.newrelic.weave.WeaveAnnotatedTypeTest$AnnotatedClass";
        final Set<String> requiredAnnotations = Sets.newHashSet(
                "com.newrelic.weave.WeaveAnnotatedTypeTest$CustomAnnotation");
        final String weaveClass = "com.newrelic.weave.WeaveAnnotatedTypeTest$AnnotatedClassWeave";

        WeaveTestUtils.weaveAnnotedTypeAndAddToContextClassloader(originalClass, weaveClass, requiredAnnotations, Collections.<String>emptySet());

        final String personOriginalClass = "com.newrelic.weave.WeaveAnnotatedTypeTest$Person";
        final String personWeave = "com.newrelic.weave.WeaveAnnotatedTypeTest$PersonWeave";
        final Set<String> personAnnotation = Sets.newHashSet(
                "com.newrelic.weave.WeaveAnnotatedTypeTest$PersonAnnotation");
        WeaveTestUtils.weaveAnnotedTypeAndAddToContextClassloader(personOriginalClass, personWeave, personAnnotation, Collections.<String>emptySet());
    }

    @interface CustomAnnotation {
    }

    @CustomAnnotation
    public static class AnnotatedClass {

        public int getNumber() {
            return 3;
        }

    }

    /**
     * The following is the equivalent of what we do in {@link #beforeClass()}:
     * 
     * @WeaveWithAnnotation(annotationClasses = "com.newrelic.weave.WeaveAnnotatedTypeTest$CustomAnnotation")
     */
    public static class AnnotatedClassWeave {

        public int getNumber() {
            return 2;
        }
    }

    @Test
    public void testWeaveAnnotatedClass() {
        /**
         * Tests that we can match on an annotation.
         *
         * {@link AnnotatedClass} should match {@link AnnotatedClassWeave}
         */
        AnnotatedClass annotatedClass = new AnnotatedClass();
        assertEquals(2, annotatedClass.getNumber());
    }

    public static class ClassNotAnnotated {

        public int getNumber() {
            return 3;
        }
    }

    @Test
    public void testWeaveAnnotatedClassFails() throws IOException {
        String internalName = Type.getInternalName(AnnotatedClassWeave.class);
        WeaveViolation[] expected = { new WeaveViolation(CLASS_MISSING_REQUIRED_ANNOTATIONS, internalName) };

        final Set<String> requiredAnnotations = Sets.newHashSet(
                "com.newrelic.weave.WeaveAnnotatedTypeTest$CustomAnnotation");
        WeaveTestUtils.expectViolations(ClassNotAnnotated.class, AnnotatedClassWeave.class, false, requiredAnnotations,
                Collections.<String>emptySet(), expected);
    }

    public static @interface PersonAnnotation {
    }

    @PersonAnnotation
    public static class Person {
        private static int numCalls = 0;

        public int getX() {
            return 1;
        }

        public int getY() {
            return 2;
        }

        public int getZ() {
            return 3;
        }

        public int getNumCalls() {
            return numCalls;
        }
    }

    // @WeaveTypeWithAnnotation(annotationClasses = "com.newrelic.weave.WeaveAnnotatedTypeTest$PersonAnnotation")
    public static class PersonWeave {
        private static int numCalls = Weaver.callOriginal();

        @WeaveIntoAllMethods
        private static void weaveIntoAllMethods() {
            numCalls++;
        }
    }

    @Test
    public void testWeaveAnnotedTypeWeaveIntoAllMethods() {
        Person person = new Person();

        assertEquals(1, person.getX());
        assertEquals(2, person.getY());
        assertEquals(3, person.getZ());
        final int numCalls = person.getNumCalls();

        assertEquals(4, numCalls);
    }

}
