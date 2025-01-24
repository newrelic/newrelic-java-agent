package com.newrelic.weave.utils;

import com.sun.org.apache.bcel.internal.generic.ALOAD;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.junit.Assert.*;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;

public class ReturnInsnProcessorTest {



    //the nodes in here need to be constructed by hand since Java compilers don't have this issue

    @Test
    public void singleReturnStackClears() {
       MethodNode mn = buildMethodNode(INT_TYPE);
       buildOneUntidyReturn(mn);
       WeaveUtils.printAllInstructions(mn);

       ReturnInsnProcessor.clearReturnStacks("MyClazz", mn);
       WeaveUtils.printAllInstructions(mn);
    }

    @Test
    public void stackAlreadyClearDoesNothing() {

    }


    public MethodNode buildMethodNode(Type type) {
        String desc = "";
        desc = "()" + type.getDescriptor();
        MethodNode mn = new MethodNode(Opcodes.ASM9, ACC_PUBLIC, "myMethod", desc, null, null);
        return mn;
    }

    private void buildOneUntidyReturn(MethodNode mn) {
        //Java compilers will resist untidy return stacks.
        //But we can generate unnecessary bytecode using ASM if we want to!
        InsnList insns = new InsnList();
        //setup insns
        insns.add(new LabelNode());
        insns.add(new InsnNode(Opcodes.DCONST_1));
        insns.add(new VarInsnNode(Opcodes.DSTORE, 1));
        insns.add(new InsnNode(Opcodes.DCONST_0));
        insns.add(new InsnNode(Opcodes.DUP2));
        insns.add(new InsnNode(Opcodes.DRETURN));

        mn.instructions.add(insns);
        mn.maxLocals += 4;
        mn.maxStack += 4;
    }

    private void buildOneTidyReturn(MethodNode mn) {
        InsnList insns = new InsnList();
        //setup insns
        insns.add(new InsnNode(Opcodes.ICONST_3));
        insns.add(new VarInsnNode(Opcodes.ISTORE, 1));
        insns.add(new InsnNode(Opcodes.ICONST_4));
        insns.add(new InsnNode(Opcodes.IRETURN));

        mn.instructions.add(insns);
    }




}