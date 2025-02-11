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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.objectweb.asm.Opcodes;

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

    @Test
    public void testPrintAllInstructions(){
        //intercept std out
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        int[] opcodeList = {Opcodes.ICONST_1, Opcodes.POP, Opcodes.ICONST_2, Opcodes.DUP, Opcodes.DUP, Opcodes.IRETURN};
        MethodNode mn = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "myMethod", "()I", null, null);
        InsnList insns = mn.instructions;
        for (int opcode: opcodeList) {
            insns.add(new InsnNode(opcode));
        }
        WeaveUtils.printAllInstructions(mn);

        String actual = outContent.toString().trim().replaceAll("\\s+", " ");
        String expected = "ICONST_1 POP ICONST_2 DUP DUP IRETURN";
        Assert.assertEquals(expected, actual);

        //replace std out
        System.setOut(originalOut);
    }
}
