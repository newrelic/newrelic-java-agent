/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder.patcher;

import com.nr.builder.Const;
import com.nr.builder.Patcher;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Removes ElementType.MODULE and ElementType.RECORD_COMPONENT from the @Target annotation on
 * shaded jspecify NullMarked. Both enum values were introduced after Java 8 and cause
 * ArrayStoreException in Java 8's AnnotationParser when a Spring scans the agent JAR.
 */
public class RemoveUnsupportedAnnotationTargets implements Patcher {

    private static final String NULL_MARKED_INTERNAL_NAME =
            "com/newrelic/agent/deps/caffeine3/org/jspecify/annotations/NullMarked";
    private static final String TARGET_DESCRIPTOR = "Ljava/lang/annotation/Target;";

    /**
     * This will set shouldTransform() to true if this is our target NullMarked class
     */
    @Override
    public ClassVisitor getVerificationVisitor(ClassVisitor next, AtomicBoolean shouldTransform) {
        return new ClassVisitor(Const.ASM_API, next) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (NULL_MARKED_INTERNAL_NAME.equals(name)) {
                    shouldTransform.set(true);
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
    }

    /**
     * Register visitors for the class, @Target annotation, @Target array and enum to
     * strip out the unneeded enum values.
     */
    @Override
    public ClassVisitor getRewritingVisitor(ClassVisitor next) {
        return new ClassVisitor(Const.ASM_API, next) {
            private boolean isNullMarked = false;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                isNullMarked = NULL_MARKED_INTERNAL_NAME.equals(name);
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
                if (!isNullMarked || !TARGET_DESCRIPTOR.equals(descriptor)) {
                    return av;
                }
                return new AnnotationVisitor(Const.ASM_API, av) {
                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        AnnotationVisitor arrayAv = super.visitArray(name);
                        return new AnnotationVisitor(Const.ASM_API, arrayAv) {
                            @Override
                            public void visitEnum(String name, String descriptor, String value) {
                                if (!"MODULE".equals(value) && !"RECORD_COMPONENT".equals(value)) {
                                    super.visitEnum(name, descriptor, value);
                                }
                            }
                        };
                    }
                };
            }
        };
    }
}
