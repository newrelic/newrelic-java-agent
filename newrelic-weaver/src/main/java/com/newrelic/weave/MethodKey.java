/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import java.util.Arrays;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * Java methods must be unique based solely on name and parameter types; return type isn't a factor. This class is
 * used as a key in maps to determine matching instead of ASM Method objects because they're dependent on return
 * types.
 */
public class MethodKey {
    /**
     * The method name.
     */
    public final String name;

    /**
     * Method parameters <i>and</i> return type, i.e. the {@link Type[]} representation of {@link MethodNode#desc}.
     */
    public final Type[] argumentTypes;

    /**
     * Create a key for a {@link MethodNode}.
     * @param node ASM {@link MethodNode}
     */
    public MethodKey(MethodNode node) {
        this(node.name, node.desc);
    }

    /**
     * Create a key for the specified method name and ASM descriptor.
     * @param name ASM method name, {@link MethodNode#name}
     * @param desc ASM method descriptor, {@link MethodNode#desc}
     */
    public MethodKey(String name, String desc) {
        this.name = name;
        this.argumentTypes = Type.getArgumentTypes(desc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MethodKey methodKey = (MethodKey) o;
        return name.equals(methodKey.name) && Arrays.deepEquals(argumentTypes, methodKey.argumentTypes);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(argumentTypes);
        return result;
    }
}
