/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * This class exists only to work around the fact that MethodNode instances are NOT thread safe. If multiple threads
 * call {@link #accept(ClassVisitor)} or {@link #accept(MethodVisitor)} at the same time there is a race condition that
 * exists that has been shown to cause VerifyErrors and other serious bytecode-related exceptions.
 *
 * By synchronizing on the {@link #accept(ClassVisitor)} and {@link #accept(MethodVisitor)} methods we are ensuring that
 * any usage of this MethodNode in the agent can be accessed by multiple threads. One important thing to note is that
 * this ties us pretty strongly to an ASM version due to the fact that we had to override constructors in the original
 * asm class. If these constructors change or other accept() methods are added we will need to update accordingly.
 *
 * NOTE: If you are upgrading org.ow2.asm:asm / org.ow2.asm:asm-tree make sure to double
 * check that this class hasn't changed between versions.
 */
public class SynchronizedMethodNode extends MethodNode {

    public SynchronizedMethodNode() {
        super(WeaveUtils.ASM_API_LEVEL);
    }

    public SynchronizedMethodNode(int api) {
        super(api);
    }

    public SynchronizedMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
        super(WeaveUtils.ASM_API_LEVEL, access, name, desc, signature, exceptions);
    }

    public SynchronizedMethodNode(int api, int access, String name, String desc, String signature, String[] exceptions) {
        super(api, access, name, desc, signature, exceptions);
    }

    /**
     * This method is synchronized in order to prevent issues with bytecode modification that
     * occur when multiple threads call accept on the same MethodNode instance.
     */
    @Override
    public synchronized void accept(ClassVisitor cv) {
        super.accept(cv);
    }

    /**
     * This method is synchronized in order to prevent issues with bytecode modification that
     * occur when multiple threads call accept on the same MethodNode instance.
     */
    @Override
    public synchronized void accept(MethodVisitor mv) {
        super.accept(mv);
    }

}
