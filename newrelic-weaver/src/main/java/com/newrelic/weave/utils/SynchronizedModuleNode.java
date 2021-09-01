/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;

import java.util.ArrayList;
import java.util.List;

/**
 * This class exists only to work around the fact that ModuleNode instances are NOT thread safe. If multiple threads
 * call {@link #accept(ClassVisitor)} at the same time there is a race condition that exists that has been shown to
 * cause VerifyErrors and other serious bytecode-related exceptions.
 *
 * By synchronizing on the {@link #accept(ClassVisitor)} method we are ensuring that any usage of this ModuleNode
 * in the agent can be accessed by multiple threads. One important thing to note is that this ties us pretty strongly to
 * an ASM version due to the fact that we had to override constructors in the original asm class. If these constructors
 * change or other accept() methods are added we will need to update accordingly.
 *
 * NOTE: If you are upgrading from org.ow2.asm:asm / org.ow2.asm:asm-tree make sure to double
 * check that this class hasn't changed between versions.
 */
public class SynchronizedModuleNode extends ModuleNode {

    public SynchronizedModuleNode(final String name, final int access, final String version) {
        super(name, access, version);
    }

    public SynchronizedModuleNode(final int api, final String name, final int access, final String version, final List<ModuleRequireNode> requires,
            final List<ModuleExportNode> exports, final List<ModuleOpenNode> opens, final List<String> uses, final List<ModuleProvideNode> provides) {
        super(api, name, access, version, requires, exports, opens, uses, provides);
    }

    @Override
    public void visitRequire(String module, int access, String version) {
        if (requires == null) {
            requires = new ArrayList<>(5);
        }
        requires.add(new SynchronizedModuleRequireNode(module, access, version));
    }

    @Override
    public void visitExport(String packaze, int access, String... modules) {
        if (exports == null) {
            exports = new ArrayList<>(5);
        }
        List<String> moduleList = null;
        if (modules != null) {
            moduleList = new ArrayList<>(modules.length);
            for (int i = 0; i < modules.length; i++) {
                moduleList.add(modules[i]);
            }
        }
        exports.add(new SynchronizedModuleExportNode(packaze, access, moduleList));
    }

    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        if (opens == null) {
            opens = new ArrayList<>(5);
        }
        List<String> moduleList = null;
        if (modules != null) {
            moduleList = new ArrayList<>(modules.length);
            for (int i = 0; i < modules.length; i++) {
                moduleList.add(modules[i]);
            }
        }
        opens.add(new SynchronizedModuleOpenNode(packaze, access, moduleList));
    }

    @Override
    public void visitProvide(String service, String... providers) {
        if (provides == null) {
            provides = new ArrayList<>(5);
        }
        List<String> providerList = null;
        if (providers != null) {
            providerList = new ArrayList<>(providers.length);
            for (int i = 0; i < providers.length; i++) {
                providerList.add(providers[i]);
            }
        }
        provides.add(new SynchronizedModuleProvideNode(service, providerList));
    }

    /**
     * This method is synchronized in order to prevent issues with bytecode modification that
     * occur when multiple threads call accept on the same SynchronizedModuleNode instance.
     */
    @Override
    public synchronized void accept(ClassVisitor cv) {
        super.accept(cv);
    }

}
