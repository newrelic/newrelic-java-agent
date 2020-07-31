/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.newrelic.weave.violation.WeaveViolationType;

public class ClassMatchNewInnerClassTest {

    public static class Original {
        public static int allowedStaticField;
        private static int notAllowedStaticField;
        public int allowedMemberField;
        private int notAllowedMemberField;

        private static void notAllowedStaticMethod() {
        }

        public static void allowedStaticMethod() {
        }

        private void notAllowedMemberMethod() {
        }

        public void allowedMemberMethod() {
        }
    }

    public static class Weave {
        public static int allowedStaticField;
        private static int notAllowedStaticField;
        public int allowedMemberField;
        private int notAllowedMemberField;

        private int allowedNewField; // this is a new field - which must be private - and is supported

        private void notAllowedNewMethod() {
            // new methods are never allowed to be accessed in nested classes
            // this is because they can access non-public members in the original class
            // we decided it is simpler to disallow ALL new methods in nested classes
            // rather than try to allow those that only access public members
        }

        private static void notAllowedStaticMethod() {
        }

        public static void allowedStaticMethod() {
        }

        private void notAllowedMemberMethod() {
        }

        public void allowedMemberMethod() {
        }

        public static class AllowedStaticFieldClass {
            public int field = allowedStaticField;
        }

        public static class NotAllowedStaticFieldClass {
            public int field = notAllowedStaticField;
        }

        public class AllowedMemberFieldClass {
            public int field = allowedMemberField;
        }

        public class NotAllowedMemberFieldClass {
            public int field = notAllowedMemberField;
        }

        public static class AllowedStaticMethodClass {
            public static void method() {
                allowedStaticMethod();
            }
        }

        public static class NotAllowedStaticMethodClass {
            public static void method() {
                notAllowedStaticMethod();
            }
        }

        public class AllowedMemberMethodClass {
            public void method() {
                allowedMemberMethod();
            }
        }

        public class NotAllowedMemberMethodClass {
            public void method() {
                notAllowedMemberMethod();
            }
        }

        public class AllowedNewFieldClass {
            public int field = allowedNewField;
        }

        public class NotAllowedNewMethodClass {
            public void method() {
                notAllowedNewMethod();
            }
        }
    }

    public ClassMatch match;

    @Before
    public void before() throws IOException {
        match = WeaveTestUtils.match(Original.class, Weave.class, false);
    }

    @Test
    public void testNotAllowedStaticFieldClass() throws IOException {
        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.AllowedStaticFieldClass.class));
        assertEquals(0, match.getViolations().size());

        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.NotAllowedStaticFieldClass.class));
        assertEquals(1, match.getViolations().size());
        assertEquals(WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED, Iterables.getOnlyElement(
                match.getViolations()).getType());
    }

    @Test
    public void testNotAllowedMemberFieldClass() throws IOException {
        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.AllowedMemberFieldClass.class));
        assertEquals(0, match.getViolations().size());

        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.NotAllowedMemberFieldClass.class));
        assertEquals(1, match.getViolations().size());
        assertEquals(WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED, Iterables.getOnlyElement(
                match.getViolations()).getType());
    }

    @Test
    public void testNotAllowedStaticMethodClass() throws IOException {
        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.AllowedStaticMethodClass.class));
        assertEquals(0, match.getViolations().size());

        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.NotAllowedStaticMethodClass.class));
        assertEquals(1, match.getViolations().size());
        assertEquals(WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED, Iterables.getOnlyElement(
                match.getViolations()).getType());
    }

    @Test
    public void testNotAllowedMemberMethodClass() throws IOException {
        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.AllowedMemberMethodClass.class));
        assertEquals(0, match.getViolations().size());

        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.NotAllowedMemberMethodClass.class));
        assertEquals(1, match.getViolations().size());
        assertEquals(WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED, Iterables.getOnlyElement(
                match.getViolations()).getType());
    }

    @Test
    public void testAllowedNewFieldClass() throws IOException {
        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.AllowedNewFieldClass.class));
        assertEquals(0, match.getViolations().size());
    }

    @Test
    public void testNotAllowedNewMethodClass() throws IOException {
        match.validateNewInnerClass(WeaveTestUtils.readClass(Weave.NotAllowedNewMethodClass.class));
        assertEquals(1, match.getViolations().size());
        assertEquals(WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED, Iterables.getOnlyElement(
                match.getViolations()).getType());
    }
}
