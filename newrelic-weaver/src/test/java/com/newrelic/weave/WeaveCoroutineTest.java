/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/*
This test checks whether weaving is successful for a class known to generate bytecode with extra operands on the stack.
 */
public class WeaveCoroutineTest {

    private static ClassNode original1;
    private static ClassNode composite1;
    private static ClassNode original2;
    private static ClassNode composite2;

    @BeforeClass
    public static void before() throws IOException {
        System.setProperty("newrelic.config.class_transformer.clear_return_stacks", "true");
        original1 = WeaveTestUtils.readClass("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doOneSuspend$1$1");
        ClassWeave weave1 = WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doOneSuspend$1$1",
                "com.newrelic.weave.weavepackage.testclasses.Weave_SampleCoroutine");
        composite1 = weave1.getComposite();

        original2 = WeaveTestUtils.readClass("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doThreeSuspends$1$1");
        ClassWeave weave2 = WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doThreeSuspends$1$1",
                "com.newrelic.weave.weavepackage.testclasses.Weave_SampleCoroutine");
        composite2 = weave2.getComposite();
    }


    @Before
    public void beforeEach() {
        System.setProperty("newrelic.config.class_transformer.clear_return_stacks", "true");

    }
    @After
    public void after() {
        System.clearProperty("newrelic.config.class_transformer.clear_return_stacks");
    }

    //This test is the important one.
    //If the weaver doesn't clean the return stacks of these coroutines, the test will fail with an ArrayIndexOOB Error.
    @Test
    public void weavedCoroutineShouldNotThrow() {
        SampleCoroutineKt.doOneSuspend();
        SampleCoroutineKt.doThreeSuspends();
    }

    @Test
    public void nestedSuspendsShouldNotThrow() throws IOException {
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doNestedSuspends$1$1",
                "com.newrelic.weave.weavepackage.testclasses.Weave_SampleCoroutine");
        SampleCoroutineKt.doNestedSuspends();
    }

    //The original method and weaved method are hard to compare directly, because the weaved method includes
    //a bunch of extra New Relic Stuff. This just checks we added some pops (as expected).
    @Test
    public void weavedCoroutineAddsOnePop() {
        MethodNode originalInvokeSuspend = getNodeNamed(original1.methods, "invokeSuspend");
        assertNotNull(originalInvokeSuspend);

        MethodNode weavedInvokeSuspend = getNodeNamed(composite1.methods, "invokeSuspend");
        assertNotNull(weavedInvokeSuspend);

        int popsBefore = countInsnsWithOpcode(originalInvokeSuspend, Opcodes.POP);
        int popsAfter = countInsnsWithOpcode(weavedInvokeSuspend, Opcodes.POP);

        assertEquals(popsBefore + 1, popsAfter);
    }

    @Test
    public void weavedCoroutineAddsSeveralPops() throws IOException {
        MethodNode originalInvokeSuspend = getNodeNamed(original2.methods, "invokeSuspend");
        assertNotNull(originalInvokeSuspend);

        MethodNode weavedInvokeSuspend = getNodeNamed(composite2.methods, "invokeSuspend");
        assertNotNull(weavedInvokeSuspend);

        int popsBefore = countInsnsWithOpcode(originalInvokeSuspend, Opcodes.POP);
        int popsAfter = countInsnsWithOpcode(weavedInvokeSuspend, Opcodes.POP);

        assertEquals(popsBefore + 3, popsAfter);
    }

    @Test
    public void weavedCoroutineAddsNoPops() throws IOException {
        ClassNode original = WeaveTestUtils.readClass("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doNoSuspends$1$1");
        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doNoSuspends$1$1",
                "com.newrelic.weave.weavepackage.testclasses.Weave_SampleCoroutine");
        ClassNode composite = weave.getComposite();

        MethodNode originalInvokeSuspend = getNodeNamed(original.methods, "invokeSuspend");
        assertNotNull(originalInvokeSuspend);

        MethodNode weavedInvokeSuspend = getNodeNamed(composite.methods, "invokeSuspend");
        assertNotNull(weavedInvokeSuspend);

        int popsBefore = countInsnsWithOpcode(originalInvokeSuspend, Opcodes.POP);
        int popsAfter = countInsnsWithOpcode(weavedInvokeSuspend, Opcodes.POP);

        assertEquals(popsBefore, popsAfter);
    }

    //We added a feature flag to disable the return insn processing.
    //Setting this flag should allow the original error to throw.
    @Test
    public void featureFlagNotSetThrowsAIOOBException() throws IOException {
        //This property has to be set at weave time, so it has to be run with a new class.
        System.setProperty("newrelic.config.class_transformer.clear_return_stacks", "false");

        try {
            WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.weavepackage.testclasses.SampleCoroutineKt$doExpectedErrorSuspend$1$1",
                    "com.newrelic.weave.weavepackage.testclasses.Weave_SampleCoroutine");
            fail("Feature flag clear_return_stacks should throw AIOOBException during weaving.");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

    }

    private MethodNode getNodeNamed(List<MethodNode> methods, String targetName) {
        for (MethodNode mn : methods) {
            if (mn.name.equals(targetName)) {
                return mn;
            }
        }
        return null;
    }

    private int countInsnsWithOpcode(MethodNode mn, int opcode) {
        int count = 0;
        for (AbstractInsnNode insn : mn.instructions) {
            if (insn.getOpcode() == opcode) {
                count++;
            }
        }
        return count;
    }

}