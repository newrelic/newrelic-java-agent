/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder.patcher;

import com.nr.builder.Const;
import com.nr.builder.Patcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.nr.builder.Log4j2PluginFileMover.LOG4J2PLUGINS_NEW_LOCATION;
import static com.nr.builder.Log4j2PluginFileMover.LOG4J2PLUGINS_ORIGINAL_LOCATION;

/**
 * This class is different from {@link com.nr.builder.Log4j2PluginFileMover} -
 * Change any <i>String</i> references within class files to the Log4j2Plugins.dat
 * file to the new location.
 */
public class ModifyReferencesToLog4j2Plugins implements Patcher {
    @Override
    public ClassVisitor getVerificationVisitor(ClassVisitor next, AtomicBoolean shouldTransform) {
        return new LdcTransformingClassVisitor(next, currentValue -> {
            shouldTransform.set(true);
            return currentValue;
        });
    }

    @Override
    public ClassVisitor getRewritingVisitor(ClassVisitor next) {
        return new LdcTransformingClassVisitor(next, currentValue -> LOG4J2PLUGINS_NEW_LOCATION);
    }

    /**
     * Maps the current value of every LDC instruction matching {@link com.nr.builder.Log4j2PluginFileMover#LOG4J2PLUGINS_ORIGINAL_LOCATION} to the provided new value.
     */
    private static class LdcTransformingClassVisitor extends ClassVisitor {
        private final Function<String, String> getNewValue;

        public LdcTransformingClassVisitor(ClassVisitor next, Function<String, String> getNewValue) {
            super(Const.ASM_API, next);
            this.getNewValue = getNewValue;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof String) {
                        String constant = (String) value;
                        if (constant.equals(LOG4J2PLUGINS_ORIGINAL_LOCATION)) {
                            value = getNewValue.apply(constant);
                        }
                    }
                    super.visitLdcInsn(value);
                }
            };
        }
    }
}
