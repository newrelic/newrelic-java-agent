package com.newrelic.weave.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.HashMap;
import java.util.Map;

public class ReturnInsnProcessor {
    /***
     * Tidy up the stacks at every return instruction of a method.
     *
     * @param owner The owning class of the method
     * @param mn The method
     * @return mn, with updated instructions
     */

    public static MethodNode clearReturnStacks(String owner, MethodNode mn) {
        int maxLocals = mn.maxLocals;
        int nextLocalIdx = maxLocals; //create a new local variable
        Map<AbstractInsnNode, Integer> stacks;
        try {
            stacks = calculateReturnStacks(owner, mn);
        } catch (AnalyzerException e) {
            //Something went wrong calculating return stacks, send back the method unmodified.
            return mn;
        }
        for (Map.Entry<AbstractInsnNode, Integer> entry : stacks.entrySet()) {
            AbstractInsnNode insn = entry.getKey();
            Integer stackSize = entry.getValue();
            InsnList clearInsns = insnsBeforeReturn(insn.getOpcode(), stackSize, nextLocalIdx);
            mn.instructions.insertBefore(insn, clearInsns);
        }
        mn.maxLocals = nextLocalIdx + 1;
        return mn;
    }

    /*
    The issue has only been observed happening with reference (A-type) instructions in coroutines.
    But for safety (and for reuse in future contexts), this method
    checks the type of the return and pairs it with the appropriate store and load types.
     */

    private static InsnList insnsBeforeReturn(int opcode, int stackSize, int varIndex){
        InsnList insns = new InsnList();
//        if (opcode == Opcodes.RETURN) {
//            for (int i = stackSize; i > 0; i--) {
//                insns.add(new InsnNode(Opcodes.POP));
//            }
//            return insns;
//        }
        int store;
        int load;
        switch (opcode) {
            case Opcodes.ARETURN:
                store = Opcodes.ASTORE;
                load = Opcodes.ALOAD;
                break;
            case Opcodes.IRETURN :
                store = Opcodes.ISTORE;
                load = Opcodes.ILOAD;
                break;
            case Opcodes.DRETURN:
                store = Opcodes.DSTORE;
                load = Opcodes.DLOAD;
                break;
            case Opcodes.LRETURN:
                store = Opcodes.LSTORE;
                load = Opcodes.LLOAD;
                break;
            case Opcodes.FRETURN:
                store = Opcodes.FSTORE;
                load = Opcodes.FLOAD;
                break;
            default:
                return insns;
        }
        insns.add(new VarInsnNode(store, varIndex));
        for (int i = stackSize; i > 1; i--){
            insns.add(new InsnNode(Opcodes.POP));
        }
        insns.add(new VarInsnNode(load, varIndex));
        return insns;
    }

    /***
     * Compute the size of the operand stack at each return instruction.
     * @param owner Owning class
     * @param method The method to analyze
     * @return A Map of return instruction instances and their stack sizes
     */

    private static Map<AbstractInsnNode, Integer> calculateReturnStacks(String owner, MethodNode method) throws AnalyzerException {
        BasicInterpreter interpreter = new BasicInterpreter();
        Analyzer<BasicValue> a = new Analyzer<>(interpreter);
        Frame<BasicValue>[] frames;
        frames = a.analyze(owner, method);
        Map<AbstractInsnNode, Integer> rtStacks = new HashMap<>();
        for (int j = 0; j < method.instructions.size(); ++j) {
            AbstractInsnNode insn = method.instructions.get(j);
            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.ARETURN) {
                Frame<BasicValue> f = frames[j];
                if (f != null && f.getStackSize() > 1) {
                    rtStacks.put(insn, f.getStackSize());
                }
            }
        }
        return rtStacks;
    }

}
