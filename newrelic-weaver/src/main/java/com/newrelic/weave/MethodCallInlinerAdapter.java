/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.ReturnInsnProcessor;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public abstract class MethodCallInlinerAdapter extends LocalVariablesSorter {
    /**
     * try/catch blocks which originated from the inlined method.
     */
    private List<TryCatchBlockNode> inlinedTryCatchBlocks = null;

    /*
     * Only used if the inlineFrames option is true. Allows the computation of the stack map frames at any instruction
     * of the caller method, and in particular at the call sites to methods to be inlined (needed to merge this caller
     * stack maps with the callee ones).
     */
    private final AnalyzerAdapter analyzerAdapter;

    /*
     * If a method to be inlined is called several times from the same caller method, we don't want to allocate new
     * local variables for each invocation. Instead, we want to share the local slots between all call sites. To do this
     * we need to reuse the InliningAdapter instance, which maintains (as a LocalVariablesSorter subclass) the mapping
     * from the original locals in the method to be inlined to the locals in the caller method. This is the goal of this
     * map.
     */
    private Map<String, InlinedMethod> inliners;

    public static class InlinedMethod {

        // The code that must be inlined.
        public final MethodNode method;

        // How the code must be remapped to be inlined.
        public final Remapper remapper;

        InliningAdapter inliner;

        public InlinedMethod(final MethodNode method, final Remapper remapper) {
            this.method = method;
            this.remapper = remapper;
        }
    }

    static InlinedMethod DO_NOT_INLINE = new InlinedMethod(null, null);

    // If inlineFrame is true, frames from the calling method and from
    // the inlined methods are merged during inlining, thus avoiding the
    // need to compute all frames from scratch. But this requires both
    // methods to already have computed frames.
    // If inlineFrame is false the frames are not changed at all, thus
    // they must be computed from scratch in the ClassWriter.
    public MethodCallInlinerAdapter(String owner, int access, String name, String desc, MethodVisitor next,
            boolean inlineFrames) {
        this(WeaveUtils.ASM_API_LEVEL, owner, access, name, desc, next, inlineFrames);
    }

    protected MethodCallInlinerAdapter(int api, String owner, int access, String name, String desc, MethodVisitor next,
            boolean inlineFrames) {
        super(api, access, desc, getNext(owner, access, name, desc, next, inlineFrames));
        this.analyzerAdapter = inlineFrames ? (AnalyzerAdapter) mv : null;
        this.mv = new InlinedTryCatchBlockSorter(WeaveUtils.ASM_API_LEVEL, this.mv, access, name, desc, null, null);
    }

    private static MethodVisitor getNext(String owner, int access, String name, String desc, MethodVisitor next,
            boolean inlineFrames) {
        MethodVisitor mv = next;
        if (inlineFrames) {
            mv = new AnalyzerAdapter(owner, access, name, desc, mv);
        }
        return mv;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        InlinedMethod inliner = getInliner(owner, name, desc);
        if (inliner == DO_NOT_INLINE) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        if (inliner.inliner == null) {
            MethodVisitor mv = this.mv;
            if (analyzerAdapter != null) {
                mv = new MergeFrameAdapter(api, analyzerAdapter, mv);
            }
            if (inliner.method.tryCatchBlocks != null && !inliner.method.tryCatchBlocks.isEmpty()) {
                if (this.inlinedTryCatchBlocks == null) {
                    this.inlinedTryCatchBlocks = new ArrayList<>();
                }
                this.inlinedTryCatchBlocks.addAll(inliner.method.tryCatchBlocks);
            }
            int access = opcode == Opcodes.INVOKESTATIC ? Opcodes.ACC_STATIC : 0;
            inliner.inliner = new InliningAdapter(api, access, desc, this, mv, inliner.remapper);
        }
        inliner.method.accept(inliner.inliner);
    }

    protected abstract InlinedMethod mustInline(String owner, String name, String desc);

    private InlinedMethod getInliner(String owner, String name, String desc) {
        if (inliners == null) {
            inliners = new HashMap<>();
        }
        String key = owner + "." + name + desc;
        InlinedMethod method = inliners.get(key);
        if (method == null) {
            method = mustInline(owner, name, desc);
            if (method == null) {
                method = DO_NOT_INLINE;
            } else {
                // Copy the MethodNode before modifying the instructions list (which is not thread safe)
                MethodNode methodNodeCopy = WeaveUtils.copy(method.method);
                if (shouldClearReturnStacks(name, desc)) {
                    MethodNode result = WeaveUtils.newMethodNode(methodNodeCopy);
                    methodNodeCopy.accept(new ClearReturnAdapter(owner, methodNodeCopy, result));
                    methodNodeCopy = result;
                }
                methodNodeCopy.instructions.resetLabels();
                InliningAdapter originalInliner = method.inliner;

                // Replace the InlinedMethod instance with our new values and set the inliner
                method = new InlinedMethod(methodNodeCopy, method.remapper);
                method.inliner = originalInliner;
            }
            inliners.put(key, method);
        }
        return method;
    }

    class InliningAdapter extends LocalVariablesSorter {

        private final int access;

        private final String desc;

        private final LocalVariablesSorter caller;

        private Label end;

        public InliningAdapter(int api, int access, String desc, LocalVariablesSorter caller, MethodVisitor next,
                Remapper remapper) {
            super(api, access, desc, new MethodRemapper(next, remapper));
            this.access = access;
            this.desc = desc;
            this.caller = caller;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            int off = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
            Type[] args = Type.getArgumentTypes(desc);

            int argRegister = off;
            for (int i = 0; i < args.length; ++i) {
                argRegister += args[i].getSize();
            }
            for (int i = args.length - 1; i >= 0; i--) {
                argRegister -= args[i].getSize();
                visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), argRegister);
            }
            if (off > 0) {
                visitVarInsn(Opcodes.ASTORE, 0);
            }
            end = new Label();
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                super.visitJumpInsn(Opcodes.GOTO, end);
            } else {
                super.visitInsn(opcode);
            }
        }

        @Override
        public void visitVarInsn(final int opcode, final int var) {
            // Offset all locals by firstLocal to also remap the method
            // arguments
            super.visitVarInsn(opcode, var + firstLocal);
        }

        @Override
        public void visitIincInsn(final int var, final int increment) {
            // Offset all locals by firstLocal to also remap the method
            // arguments
            super.visitIincInsn(var + firstLocal, increment);
        }

        @Override
        public void visitLocalVariable(final String name, final String desc, final String signature, final Label start,
                final Label end, final int index) {
            // Offset all locals by firstLocal to also remap the method
            // arguments
            super.visitLocalVariable(name, desc, signature, start, end, index + firstLocal);
        }

        @Override
        public void visitMaxs(int stack, int locals) {
            super.visitLabel(end);
        }

        @Override
        public void visitEnd() {
            // Do nothing
        }

        @Override
        protected int newLocalMapping(final Type type) {

            // REVIEW this code originally wanted to reflect to invoke newLocalMapping. That causes
            // an access exception with the WebSphere security manager. The newLocal method seems to
            // do about the same thing but a little more. anyway, the tests pass..
            return caller.newLocal(type);
        }
    }

    /**
     * Flags method nodes requiring additional return insn processing.
     *
     * It returns false by default.
     *
     * To enable this feature, set -Dnewrelic.config.class_transformer.clear_return_stacks=true
     * For future reference, if other code surfaces a bytecode verification failure (such as an ArrayIndexOutOfBoundsException),
     * modify this guard to process additional methods.
     */
    private boolean shouldClearReturnStacks(String name, String desc) {
        String enabled = System.getProperty("newrelic.config.class_transformer.clear_return_stacks", "false");
        return enabled.equalsIgnoreCase("true");
    }

//    /**
//     * Feature flag to disable return stack processing.
//     * To use this feature flag, set -Dnewrelic.config.class_transformer.clear_return_stacks=false at JVM startup.
//     */
//    private boolean clearReturnStacksDisabled() {
//        String enabled = System.getProperty("newrelic.config.class_transformer.clear_return_stacks", "true");
//        return enabled.equalsIgnoreCase("false");
//    }

    /**
     * This adapter checks a method's return instructions, adding additional POPs prior to return instructions
     * if the return is made with extra (>1) operands on the stack.
     * <p>
     * ReturnInsnProcessor.clearReturnStacks is placed in visitEnd() of this adapter, rather than called directly on
     * the source node, for thread safety reasons. Our thread safety strategy is to synchronize on the accept method
     * of the source node, and require that all modifications to bytecode be initiated by an invocation of accept():
     * <p>
     * source.accept(new ClearReturnAdapter(owner, source, next)) //conforms to thread-safe model
     * <p>
     * ReturnInsnProcessor.clearReturnStacks(owner, source) //does not conform to thread-safe model
     */
    class ClearReturnAdapter extends MethodNode {
        String owner;

        public ClearReturnAdapter(String owner, MethodNode source, MethodVisitor next) {
            super(WeaveUtils.ASM_API_LEVEL, source.access, source.name, source.desc, source.signature,
                    source.exceptions.toArray(new String[source.exceptions.size()]));
            this.mv = next;
            this.owner = owner;
        }

        @Override
        public void visitEnd() {
            ReturnInsnProcessor.clearReturnStacks(owner, this);
            accept(mv);
        }
    }

    static class MergeFrameAdapter extends MethodVisitor {

        private final AnalyzerAdapter analyzerAdapter;

        public MergeFrameAdapter(int api, AnalyzerAdapter analyzerAdapter, MethodVisitor next) {
            super(api, next);
            this.analyzerAdapter = analyzerAdapter;
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            List<Object> callerLocal = analyzerAdapter.locals;
            int nCallerLocal = callerLocal == null ? 0 : callerLocal.size();
            int nMergedLocal = Math.max(nCallerLocal, nLocal);
            Object[] mergedLocal = new Object[nMergedLocal];
            for (int i = 0; i < nCallerLocal; ++i) {
                if (callerLocal.get(i) != Opcodes.TOP) {
                    mergedLocal[i] = callerLocal.get(i);
                }
            }
            for (int i = 0; i < nLocal; ++i) {
                if (local[i] != Opcodes.TOP) {
                    mergedLocal[i] = local[i];
                }
            }
            List<Object> callerStack = analyzerAdapter.stack;
            int nCallerStack = callerStack == null ? 0 : callerStack.size();
            int nMergedStack = nCallerStack + nStack;
            Object[] mergedStack = new Object[nMergedStack];
            for (int i = 0; i < nCallerStack; ++i) {
                mergedStack[i] = callerStack.get(i);
            }
            if (nStack > 0) {
                System.arraycopy(stack, 0, mergedStack, nCallerStack, nStack);
            }
            super.visitFrame(type, nMergedLocal, mergedLocal, nMergedStack, mergedStack);
        }
    }

    /**
     * Sort the try/catch blocks so that inlined blocks take priority over non-inlined blocks.
     */
    private class InlinedTryCatchBlockSorter extends MethodNode {

        public InlinedTryCatchBlockSorter(int api, final MethodVisitor mv, final int access, final String name,
                final String desc, final String signature, final String[] exceptions) {
            super(api, access, name, desc, signature, exceptions);
            this.mv = mv;
        }

        private boolean isInlinedTryCatch(TryCatchBlockNode t) {
            return null != inlinedTryCatchBlocks && inlinedTryCatchBlocks.contains(t);
        }

        @Override
        public void visitEnd() {
            if (null != inlinedTryCatchBlocks) {
                // the elements in inlinedTryCatchBlocks have been copied around by the inliner and no longer identity
                // match what is in tryCatchBlocks so we have to rebuild it.
                int numTryCatchBlocks = inlinedTryCatchBlocks.size();
                inlinedTryCatchBlocks = new ArrayList<>(numTryCatchBlocks);
                for (int i = tryCatchBlocks.size() - numTryCatchBlocks; i < tryCatchBlocks.size(); ++i) {
                    inlinedTryCatchBlocks.add(tryCatchBlocks.get(i));
                }

                Comparator<TryCatchBlockNode> comp = new Comparator<TryCatchBlockNode>() {
                    @Override
                    public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
                        boolean isT1Inlined = isInlinedTryCatch(t1);
                        boolean isT2Inlined = isInlinedTryCatch(t2);
                        if (isT1Inlined && isT2Inlined) {
                            return 0;
                        } else if (isT1Inlined) {
                            return -1;
                        } else if (isT2Inlined) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };
                Collections.sort(tryCatchBlocks, comp);
                // Updates the 'target' of each try catch block annotation.
                for (int i = 0; i < tryCatchBlocks.size(); ++i) {
                    tryCatchBlocks.get(i).updateIndex(i);
                }
                inlinedTryCatchBlocks.clear();
            }
            if (mv != null) {
                accept(mv);
            }
        }
    }

}
