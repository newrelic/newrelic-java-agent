/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.newrelic.weave.WeaveTestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;

public class WeaveUtilsTest {

    @Test
    public void testEmptyConstructor() throws IOException {
        final ClassNode emptyConstructorClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.EmptyConstructor"));
        final MethodNode emptyCtor = WeaveUtils.getMethodNode(emptyConstructorClass, "<init>", "()V");
        Assert.assertNotNull(emptyCtor);
        Assert.assertTrue(WeaveUtils.isEmptyConstructor(emptyCtor));

        final ClassNode nonEmptyConstructorClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.NonEmptyConstructor"));
        final MethodNode nonEmptyCtor = WeaveUtils.getMethodNode(nonEmptyConstructorClass, "<init>", "()V");
        Assert.assertNotNull(nonEmptyCtor);
        Assert.assertFalse(WeaveUtils.isEmptyConstructor(nonEmptyCtor));

        final ClassNode nonEmptyConstructorTwoClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.NonEmptyConstructorTwo"));
        final MethodNode nonEmptyCtorTwo = WeaveUtils.getMethodNode(nonEmptyConstructorTwoClass, "<init>", "()V");
        Assert.assertNotNull(nonEmptyCtorTwo);
        Assert.assertFalse(WeaveUtils.isEmptyConstructor(nonEmptyCtorTwo));

        final ClassNode nonEmptyConstructorParamsClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.NonEmptyConstructorParams"));
        final MethodNode nonEmptyCtorParams = WeaveUtils.getMethodNode(nonEmptyConstructorParamsClass, "<init>", "([Ljava/lang/String;)V");
        Assert.assertNotNull(nonEmptyCtorParams);
        Assert.assertFalse(WeaveUtils.isEmptyConstructor(nonEmptyCtorParams));

        final ClassNode generatedEmptyConstructorClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.GeneratedEmptyConstructor"));
        final MethodNode generatedEmptyCtor = WeaveUtils.getMethodNode(generatedEmptyConstructorClass, "<init>", "()V");
        Assert.assertNotNull(generatedEmptyCtor);
        Assert.assertTrue(WeaveUtils.isEmptyConstructor(generatedEmptyCtor));

        final ClassNode generatedEmptyConstructorStaticInitClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.EmptyConstructorStaticInit"));
        final MethodNode generatedEmptyCtorInStatic = WeaveUtils.getMethodNode(generatedEmptyConstructorStaticInitClass, "<init>", "()V");
        Assert.assertNotNull(generatedEmptyCtorInStatic);
        Assert.assertTrue(WeaveUtils.isEmptyConstructor(generatedEmptyCtorInStatic));

        final ClassNode generatedNonEmptyInnerCtorClass = WeaveUtils.convertToClassNode(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.Outer$Inner"));
        final MethodNode generatedNonEmptyInnerCtor = WeaveUtils.getMethodNode(generatedNonEmptyInnerCtorClass, "<init>", "(Lcom/newrelic/weave/weavepackage/testclasses/Outer;)V");
        Assert.assertNotNull(generatedNonEmptyInnerCtor);
        Assert.assertFalse(WeaveUtils.isEmptyConstructor(generatedNonEmptyInnerCtor));
    }

}