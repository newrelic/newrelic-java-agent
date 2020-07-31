/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashSet;
import java.util.Set;

public class CurrentTransactionRewriter {

    /**
     * Rewrite the static field references to {@literal Transaction.CURRENT} to instead call {@link Instrumentation#getTransaction()}.
     */
    public static ClassVisitor rewriteCurrentTransactionReferences(ClassVisitor cv, final ClassReader reader) {

        final Set<Method> localTransactionMethods = getLocalTransactionMethods(reader);
        if (localTransactionMethods.isEmpty()) {
            return cv;
        }

        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                Method method = new Method(name, desc);
                if (localTransactionMethods.contains(method)) {
                    mv = new RewriteVisitor(mv, access, name, desc);
                }
                return mv;
            }
        };
    }

    private static boolean isCurrentTransactionReference(int opcode, String owner, String name) {
        // Some of our tests refer to the Transaction interface's CURRENT field through the agent's concrete Transaction
        // class - that's why there are two checks on 'owner'
        return Opcodes.GETSTATIC == opcode && BridgeUtils.isTransactionType(owner)
                && BridgeUtils.CURRENT_TRANSACTION_FIELD_NAME.equals(name);
    }

    private static boolean isCurrentTxOrNullReference(int opcode, String owner, String name) {
        return Opcodes.GETSTATIC == opcode && BridgeUtils.isTransactionType(owner)
                && BridgeUtils.CURRENT_TX_OR_NULL_FIELD_NAME.equals(name);
    }

    /**
     * Build the list of methods that reference {@literal Transaction.CURRENT}.
     */
    private static Set<Method> getLocalTransactionMethods(ClassReader reader) {
        final Set<Method> methods = new HashSet<>();
        ClassVisitor cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {
            @Override
            public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                    String signature, String[] exceptions) {
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        if (isCurrentTransactionReference(opcode, owner, name) ||
                                isCurrentTxOrNullReference(opcode, owner, name)) {
                            methods.add(new Method(methodName, methodDesc));
                        }
                    }
                };
            }
        };
        reader.accept(cv, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return methods;
    }

    private static class RewriteVisitor extends GeneratorAdapter {
        protected RewriteVisitor(MethodVisitor mv, int access, String name, String desc) {
            super(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc);
        }

        /**
         * Rewrite instructions that get the static variable {@literal Transaction.CURRENT} so that they're instead a method
         * call to getTransaction().
         */
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (isCurrentTransactionReference(opcode, owner, name)) {
                getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, BridgeUtils.INSTRUMENTATION_FIELD_NAME,
                        BridgeUtils.INSTRUMENTATION_TYPE);
                BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, this, false).build().getTransaction();
            } else if (isCurrentTxOrNullReference(opcode, owner, name)) {
                getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, BridgeUtils.INSTRUMENTATION_FIELD_NAME,
                        BridgeUtils.INSTRUMENTATION_TYPE);
                BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, this, false).build().getTransactionOrNull();
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }
    }

}
