/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import org.objectweb.asm.ClassVisitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a transformation that should take place on a dependency's classfile.
 */
public interface Patcher {
    /**
     * Gets a {@link ClassVisitor} that sets an {@link AtomicBoolean} to {@literal true} if
     * this patcher should rewrite this class. It will be used to limit actual rewriting
     * only to classes that require at least one patcher.
     */
    ClassVisitor getVerificationVisitor(ClassVisitor next, AtomicBoolean shouldTransform);

    /**
     * Gets a {@link ClassVisitor} that rewrites the class bytes. Note that this method
     * <i>will</i> be called regardless of whether or not this particular patcher
     * is used for this particular class.
     */
    ClassVisitor getRewritingVisitor(ClassVisitor next);
}
