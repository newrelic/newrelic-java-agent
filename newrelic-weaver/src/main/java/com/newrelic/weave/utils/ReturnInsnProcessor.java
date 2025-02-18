package com.newrelic.weave.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.HashMap;
import java.util.Map;

public class ReturnInsnProcessor {

    private static final int EXPECTED_RETURN_STACK_SIZE = 1;

    /***
     * Clear the stacks at every A and I-type return instruction of a method, to their EXPECTED_RETURN_STACK_SIZE of 1.
     * This mutates the given MethodNode and is not thread safe.
     *
     * @param owner The owning class of the method
     * @param mn MethodNode representing the method to be modified
     */
    public static void clearReturnStacks(String owner, MethodNode mn) {
        Map<AbstractInsnNode, Integer> stacks;
        try {
            stacks = getReturnStacks(owner, mn);
        } catch (AnalyzerException e) {
            //Something went wrong calculating return stacks, leave the method unmodified.
            return;
        }
        if (stacks != null && !stacks.isEmpty()) {
            int maxLocals = mn.maxLocals;
            int nextLocalIdx = maxLocals; //create a new local variable
            for (Map.Entry<AbstractInsnNode, Integer> entry : stacks.entrySet()) {
                AbstractInsnNode insn = entry.getKey();
                Integer stackSize = entry.getValue();
                InsnList clearInsns = insnsBeforeReturn(insn.getOpcode(), stackSize, nextLocalIdx);
                mn.instructions.insertBefore(insn, clearInsns);
            }
            mn.maxLocals = nextLocalIdx + 1;
        }
    }

    /*
    The return insn issue has only been observed happening with reference (A-type) instructions in coroutines.
    But for safety (and for reuse in future contexts), this method
    checks the type of the return and pairs it with the appropriate store and load types.

    I-type is also allowed (though it hasn't been observed in the wild before).

    All other return types will return an empty InsnList.
     */

    private static InsnList insnsBeforeReturn(int opcode, int stackSize, int varIndex) {
        InsnList insns = new InsnList();
        int store;
        int load;
        switch (opcode) {
            case Opcodes.ARETURN:
                store = Opcodes.ASTORE;
                load = Opcodes.ALOAD;
                break;
            case Opcodes.IRETURN:
                store = Opcodes.ISTORE;
                load = Opcodes.ILOAD;
                break;
            default:
                return insns;
        }
        insns.add(new VarInsnNode(store, varIndex));
        for (int i = stackSize; i > EXPECTED_RETURN_STACK_SIZE; i--) {
            insns.add(new InsnNode(Opcodes.POP));
        }
        insns.add(new VarInsnNode(load, varIndex));
        return insns;
    }

    /*
     * Compute the size of the operand stack at each return instruction exceeding the EXPECTED_RETURN_STACK_SIZE of 1.
     * Only applies to methods returning I (int type) or A (reference type).
     *
     * @param owner The owning class
     * @param method The method to analyze
     * @return A Map of return instructions having extra operands on the stack, whose keys are return insn instances and values are stack sizes at the insn
     */
    private static Map<AbstractInsnNode, Integer> getReturnStacks(String owner, MethodNode method) throws AnalyzerException {
        BasicInterpreter interpreter = new BasicInterpreter();
        Analyzer<BasicValue> a = new Analyzer<>(interpreter);
        Frame<BasicValue>[] frames = a.analyze(owner, method);
        Map<AbstractInsnNode, Integer> rtStacks = new HashMap<>();
        for (int j = 0; j < method.instructions.size(); ++j) {
            AbstractInsnNode insn = method.instructions.get(j);
            if (insn.getOpcode() == Opcodes.IRETURN || insn.getOpcode() == Opcodes.ARETURN) {
                Frame<BasicValue> f = frames[j];
                if (f != null && f.getStackSize() > EXPECTED_RETURN_STACK_SIZE) {
                    rtStacks.put(insn, f.getStackSize());
                }
            }
        }
        return rtStacks;
    }

}
