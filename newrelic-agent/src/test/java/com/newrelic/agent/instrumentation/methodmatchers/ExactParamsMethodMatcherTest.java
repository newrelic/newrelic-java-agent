/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import org.junit.Assert;

import org.junit.Test;

/**
 * Tests the ExactParmsMethodMatcher.
 * 
 * @since Sep 20, 2012
 */
public class ExactParamsMethodMatcherTest {

    @Test
    public void testMatchingBasic() {
        try {
            // javap output
            // public java.lang.String testMethod(int, boolean, float, byte);
            // Signature: (IZFB)Ljava/lang/String;

            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("testMethod", "(IZFB)"
            );
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "testMethod", "(IZFB)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "testMethod", "(IZFB)Ljava/lang/String", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "testMethod", "(IZB)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "testMethod", "(IZF)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "TestMethod", "(IZFB)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "testMethod", "(IBZF)V", com.google.common.collect.ImmutableSet.<String>of()));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMatchingListsAndArrays() {
        try {
            // javap output
            // public void testing(short, int[], java.lang.String[],
            // test.something.NewClass[][])
            // (S[I[Ljava/lang/String;[[Ltest/something/NewClass;)V
            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("hello",
                    "(S[I[Ljava/lang/String;[[Ltest/something/NewClass;)");
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "(S[I[Ljava/lang/String;[[Ltest/something/NewClass;)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS,
                    "hello", "(S[I[Ljava/lang/String;[[Ltest/something/NewClass;)Ljava/lang/String", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "S[I[Ljava/lang/String;[[Ltest/something/NewClass;)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "(S[I[Ljava/lang/String;[Ltest/something/NewClass;)V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "(S[I[[Ltest/something/NewClass;[Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testParamNamesToParamDescriptorEmpty() {
        try {
            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", "()");
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "()V", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "()Ljava.util.List", com.google.common.collect.ImmutableSet.<String>of()));
            Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "(Ljava.util.List;)Ljava.util.List", com.google.common.collect.ImmutableSet.<String>of()));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testParamNamesToParamDescriptorDuplicateDiffClass() {
        try {
            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", "()");
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "()V", com.google.common.collect.ImmutableSet.<String>of()));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", "()"
            );
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "()V", com.google.common.collect.ImmutableSet.<String>of()));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testParamNamesToParamDescriptorDuplicateSame() {

        try {
            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", "()");
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "()V", com.google.common.collect.ImmutableSet.<String>of()));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            MethodMatcher matcher = ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", "()");
            // Duplicate matcher should be fine. Checking done in MethodMatcherUtility.
            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "calculate", "()V", com.google.common.collect.ImmutableSet.<String>of()));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testParamNamesToParamDescriptorNull() {
        try {
            ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", null);
            Assert.fail("Method should throw an exception.");
        } catch (Exception e) {

        }

        try {
            ExactParamsMethodMatcher.createExactParamsMethodMatcher("calculate", "");
            Assert.fail("Method should throw an exception.");
        } catch (Exception e) {

        }
    }

}
