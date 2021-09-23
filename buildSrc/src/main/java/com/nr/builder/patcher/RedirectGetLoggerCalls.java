/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder.patcher;

import com.nr.builder.Const;
import com.nr.builder.Patcher;
import org.gradle.api.logging.Logging;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Some dependency may directly invoke {@link Logger#getLogger}. This causes bad things to happen.
 * So we change the calls to {@link Logger#getGlobal()}.
 */
public class RedirectGetLoggerCalls implements Patcher {
    private static final org.slf4j.Logger logger = Logging.getLogger(RedirectGetLoggerCalls.class);

    private static final Type LOGGER_TYPE = Type.getType(Logger.class);
    private static final Type STRING_TYPE = Type.getType(String.class);

    private static final Set<Method> GET_LOGGER_METHODS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            new Method("getLogger", LOGGER_TYPE, new Type[] { STRING_TYPE }),
            new Method("getLogger", LOGGER_TYPE, new Type[] { STRING_TYPE, STRING_TYPE })
    )));

    private interface HandleMethodEncounter {
        void staticGetLoggerEncountered(GeneratorAdapter adapter, String owner, String name, Method method);
    }

    @Override
    public ClassVisitor getVerificationVisitor(ClassVisitor next, AtomicBoolean shouldTransform) {
        return new IdentifyStaticGetLoggerCalls(next, true, (adapter, owner, name, method) -> {
            logger.debug("Should adjust logger call in {}::{}", owner, name);
            shouldTransform.set(true);
        });
    }

    @Override
    public ClassVisitor getRewritingVisitor(ClassVisitor next) {
        return new IdentifyStaticGetLoggerCalls(next, false, (adapter, owner, name, method) -> {
            // pop the arguments off the stack
            for (Type ignored : method.getArgumentTypes()) {
                adapter.pop(); // all of the arguments we match are strings with a size of 1
            }
            adapter.getStatic(LOGGER_TYPE, "global", LOGGER_TYPE);

            logger.info("Adjusted logger call in {}::{}", owner, name);
        });
    }

    private static class IdentifyStaticGetLoggerCalls extends ClassVisitor {
        private final boolean shouldVisitEncounteredCalls;
        private final HandleMethodEncounter encounter;

        public IdentifyStaticGetLoggerCalls(ClassVisitor next, boolean shouldVisitEncounteredCalls, HandleMethodEncounter encounter) {
            super(Const.ASM_API, next);
            this.shouldVisitEncounteredCalls = shouldVisitEncounteredCalls;
            this.encounter = encounter;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new GeneratorAdapter(api, super.visitMethod(access, name, descriptor, signature, exceptions), access, name, descriptor) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    Method method = new Method(name, descriptor);
                    if (opcode == Opcodes.INVOKESTATIC && GET_LOGGER_METHODS.contains(method)) {
                        encounter.staticGetLoggerEncountered(this, owner, name, method);
                        if (shouldVisitEncounteredCalls) {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }
            };
        }
    }
}
