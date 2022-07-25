/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.tree.ModuleExportNode;

import java.util.List;

/**
 * This class exists only to work around the fact that ModuleExportNode instances are NOT thread safe. If multiple threads
 * call {@link #accept(ModuleVisitor)} at the same time there is a race condition that exists that has been shown to
 * cause VerifyErrors and other serious bytecode-related exceptions.
 *
 * By synchronizing on the {@link #accept(ModuleVisitor)} method we are ensuring that any usage of this ModuleExportNode
 * in the agent can be accessed by multiple threads. One important thing to note is that this ties us pretty strongly to
 * an ASM version due to the fact that we had to override constructors in the original asm class. If these constructors
 * change or other accept() methods are added we will need to update accordingly.
 *
 * NOTE: If you are upgrading from org.ow2.asm:asm / org.ow2.asm:asm-tree make sure to double
 * check that this class hasn't changed between versions.
 */
public class SynchronizedModuleExportNode extends ModuleExportNode {

    public SynchronizedModuleExportNode(final String packaze, final int access, final List<String> modules) {
        super(packaze, access, modules);
    }

    /**
     * This method is synchronized in order to prevent issues with bytecode modification that
     * occur when multiple threads call accept on the same SynchronizedModuleExportNode instance.
     */
    public synchronized void accept(final ModuleVisitor mv) {
        super.accept(mv);
    }

}
