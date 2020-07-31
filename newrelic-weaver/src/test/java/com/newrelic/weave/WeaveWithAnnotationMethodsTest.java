/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.commons.Method;

import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;

public class WeaveWithAnnotationMethodsTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        final String originalClass = "com.newrelic.weave.WeaveWithAnnotationMethodsTest$Foo_Original";
        final String weaveClass = "com.newrelic.weave.WeaveWithAnnotationMethodsTest$Foo_Instrumentation";

        WeaveTestUtils.weaveAndAddToContextClassloader(originalClass, weaveClass);

        WeaveViolation weaveViolation = new WeaveViolation(WeaveViolationType.METHOD_MISSING_REQUIRED_ANNOTATIONS,
                "com/newrelic/weave/WeaveWithAnnotationMethodsTest$Bar_Weave", new Method("one", "()I"));
        WeaveTestUtils.expectViolations(Bar_Original.class, Bar_Weave.class, false, weaveViolation);
    }

    @Target(ElementType.METHOD)
    @interface CustomMethodAnnotation {
    }

    public static class Foo_Original {

        @CustomMethodAnnotation
        public int number() {
            return 1;
        }

    }

    public static class Foo_Instrumentation {

        @WeaveWithAnnotation(
                annotationClasses = "com.newrelic.weave.WeaveWithAnnotationMethodsTest$CustomMethodAnnotation")
        public int number() {
            return 99;
        }
    }

    public static class Bar_Original {

        @CustomMethodAnnotation
        public int one() {
            return 1;
        }

    }

    public static class Bar_Weave {
        @WeaveWithAnnotation(annotationClasses = "com.this.wont.match")
        public int one() {
            return 99;
        }
    }

    @Test
    public void testWeaveAnnotatedMethods() {
        Foo_Original foo = new Foo_Original();
        assertEquals(99, foo.number());
    }

}
