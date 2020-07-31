/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.api.agent.weaver.Weaver;

public class WeaveAnnotationValuesTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        final String originalClass = "com.newrelic.weave.WeaveAnnotationValuesTest$Original";
        final String weaveClass = "com.newrelic.weave.WeaveAnnotationValuesTest$Weave";
        WeaveTestUtils.weaveAnnotedTypeAndAddToContextClassloader(originalClass, weaveClass, Sets.newSet(
                "com.newrelic.weave.WeaveAnnotationValuesTest$FirstAnnotation"), Collections.<String>emptySet());

        final String fooOriginalClass = "com.newrelic.weave.WeaveAnnotationValuesTest$Foo_Original";
        final String fooWeaveClass = "com.newrelic.weave.WeaveAnnotationValuesTest$Foo_Instrumentation";
        WeaveTestUtils.weaveAndAddToContextClassloader(fooOriginalClass, fooWeaveClass);
    }

    @interface FirstAnnotation {
        String name();
    }

    @interface SecondAnnotation {
        String a();

        int c();

        float f();

        long l();

        double d();

        short s();

        byte b();

        char ch();

        Color color();

        FirstAnnotation annotation();

        String[] strings();

        int[] numbers();

        double[] doubles();

        char[] chars();

        long[] longs();

        short[] shorts();

        float[] floats();

        byte[] bytes();

        Color[] colors();

        FirstAnnotation[] annotations();
    }

    enum Color {
        RED, YELLOW, BLUE;
    }

    @FirstAnnotation(name = "yay")
    static class Original {
        @SecondAnnotation(a = "hi", c = 27, l = 1, s = 1, d = 12, f = 12.3f, b = 1, ch = 'Z', color = Color.RED,
                annotation = @FirstAnnotation(name = "nameVal"), doubles = { 1.2, 2.2 }, numbers = { 1, 2, 3 },
                floats = { 1, 2, 3 }, shorts = { 1, 2, 3 }, bytes = { 1, 2, 3 }, longs = { 1, 2, 3 }, chars = { 'o',
                        'p' }, strings = "one", colors = Color.BLUE, annotations = @FirstAnnotation(name = "nameVal"))

        public String originalMethod() {
            return "nay";
        }

        @SecondAnnotation(annotation = @FirstAnnotation(name = "nameVal"), doubles = { 1.2, 2.2 }, numbers = { 1, 2,
                3 }, floats = { 1, 2, 3 }, shorts = { 1, 2, 3 }, bytes = { 1, 2, 3 }, longs = { 1, 2, 3 }, chars = {
                        'o', 'p' }, strings = "one", colors = Color.BLUE, color = Color.YELLOW, a = "hi", c = 27,
                l = 44, s = 1, d = 12, f = 12.3f, b = 1, ch = 'Z', annotations = @FirstAnnotation(name = "nameVal"))
        public String methodAnnotationMember() {
            return "blah";
        }
    }

    @WeaveWithAnnotation(annotationClasses = "com.newrelic.weave.WeaveAnnotationValuesTest$FirstAnnotation",
            type = MatchType.ExactClass)
    static class Weave {
        public String originalMethod() {
            FirstAnnotation annotation = Weaver.getClassAnnotation(FirstAnnotation.class);
            if (annotation != null) {
                return annotation.name();
            }
            return "fail";
        }

        public String methodAnnotationMember() {
            SecondAnnotation annotation = Weaver.getMethodAnnotation(SecondAnnotation.class);
            if (annotation != null) {
                return annotation.strings()[0];
            }
            return null;
        }

    }

    @Test
    public void testReadClassAnnotationValue() {
        Original original = new Original();
        assertEquals("yay", original.originalMethod());
        assertEquals("one", original.methodAnnotationMember());
    }

    @interface Annotation {
        int number();
    }

    static class Foo_Original {
        static int sum = 0;

        @Annotation(number = 101)
        public int getNumber() {
            return 0;
        }

        @Annotation(number = 99)
        public String getString() {
            return null;
        }

    }

    @WeaveWithAnnotation(annotationClasses = "com.newrelic.weave.WeaveAnnotationValuesTest$Marker",
            type = MatchType.ExactClass)
    static class Foo_Instrumentation {
        static int sum = Weaver.callOriginal();

        @WeaveIntoAllMethods
        @WeaveWithAnnotation(annotationClasses = "com.newrelic.weave.WeaveAnnotationValuesTest$Annotation",
                type = MatchType.ExactClass)
        private static void instrumentation() {
            Annotation annotation = Weaver.getMethodAnnotation(Annotation.class);
            if (annotation != null) {
                sum += annotation.number();
            }
        }
    }

    @Test
    public void testGetAnnotationMemberInsideWeaveAllMethod() {
        Foo_Original foo = new Foo_Original();
        foo.getNumber();
        foo.getString();
        assertEquals(200, foo.sum);
    }
}
