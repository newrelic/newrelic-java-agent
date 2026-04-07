/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.POP;

public class ReturnInsnProcessorTest {
    //the nodes in here need to be constructed by hand since Java compilers don't have this issue
    @Test
    public void doesNotAlterAlreadyClearStack() {
        int[] originalSeq = { ICONST_1, DUP, POP, IRETURN };
        MethodNode mn = basicMethodFromSequence(Type.INT, originalSeq, 2);
        ReturnInsnProcessor.clearReturnStacks("MyClazz", mn);
        assertInsnSequence(originalSeq, mn.instructions);

        int[] originalSeqRefType = { ACONST_NULL, ARETURN };
        MethodNode mn2 = basicMethodFromSequence(Type.OBJECT, originalSeqRefType, 1);
        ReturnInsnProcessor.clearReturnStacks("MyClazz", mn2);
        assertInsnSequence(originalSeqRefType, mn2.instructions);
    }

    @Test
    public void addsPopsForIntReturnType() {
        int[] originalInsnSequence = { ICONST_2, ICONST_1, DUP, DUP, ICONST_3, IRETURN };
        MethodNode mn = basicMethodFromSequence(Type.INT, originalInsnSequence, 5);

        ReturnInsnProcessor.clearReturnStacks("MyClazz", mn);
        int[] expectedInsnSequence = { ICONST_2, ICONST_1, DUP, DUP, ICONST_3, ISTORE, POP, POP,
                POP, POP, ILOAD, IRETURN };
        assertInsnSequence(expectedInsnSequence, mn.instructions);
    }

    @Test
    public void addsPopsForReferenceReturnType() {
        int[] originalInsnSequence = { ACONST_NULL, DUP, DUP, DUP, ARETURN };
        MethodNode mn = basicMethodFromSequence(Type.OBJECT, originalInsnSequence, 4);

        ReturnInsnProcessor.clearReturnStacks("MyClazz", mn);
        int[] expectedInsnSequence = { ACONST_NULL, DUP, DUP, DUP, ASTORE, POP, POP, POP,
                ALOAD, ARETURN };
        assertInsnSequence(expectedInsnSequence, mn.instructions);
    }

    @Test
    public void handlesMultipleReturns() {
        //This Insn List is more complicated, so we have to build it directly here
        InsnList insns = new InsnList();
        insns.add(new InsnNode(ICONST_1));
        insns.add(new InsnNode(DUP));
        LabelNode label = new LabelNode();
        insns.add(new JumpInsnNode(IFEQ, label));
        insns.add(new InsnNode(IRETURN));
        insns.add(label);
        insns.add(new InsnNode(DUP));
        insns.add(new InsnNode(IRETURN));
        MethodNode mn = buildMethodNode(Type.INT);
        mn.instructions.add(insns);
        mn.maxLocals = 1;
        mn.maxStack = 2;

        ReturnInsnProcessor.clearReturnStacks("MyClazz", mn);
        int[] expectedInsns = { ICONST_1, DUP, IFEQ, IRETURN, -1, DUP, ISTORE, POP, ILOAD,
                IRETURN };
        assertInsnSequence(expectedInsns, mn.instructions);
    }

    @Test
    public void failedAnalyzerDoesNothing() {
        //these instructions are bad and will fail the analyzer - we shouldn't touch them.
        int[] originalInsns = { ICONST_1, POP, POP, IRETURN };
        MethodNode mn = basicMethodFromSequence(Type.INT, originalInsns, 1);
        ReturnInsnProcessor.clearReturnStacks("MyClazz", mn);
        assertInsnSequence(originalInsns, mn.instructions);
    }

    private void assertInsnSequence(int[] expectedSequence, InsnList insns) {
        Iterator<AbstractInsnNode> it = insns.iterator();
        int i = 0;
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();
            int expectedOpcode = expectedSequence[i];
            assertEquals(expectedOpcode, insn.getOpcode());
            i++;
        }
    }

    private MethodNode basicMethodFromSequence(int type, int[] insnSequence, int maxStack) {
        MethodNode mn = buildMethodNode(type);
        for (int opcode : insnSequence) {
            mn.instructions.add(new InsnNode(opcode));
        }
        mn.maxStack = maxStack;
        mn.maxLocals = 1;
        return mn;
    }

    public MethodNode buildMethodNode(int type) {
        String desc = "";
        switch (type) {
            case Type.INT:
                desc = "()I";
                break;
            case Type.OBJECT:
                desc = "()L";
                break;
            default:
                fail();
        }
        MethodNode mn = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "myMethod", desc, null, null);
        return mn;
    }
}