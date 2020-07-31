/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

class AgentClassWriter extends ClassWriter {
    private ClassLoader classLoader;

    public AgentClassWriter(ClassReader classReader, int flags, ClassLoader loader) {
        super(classReader, flags);
        this.classLoader = loader;
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        ClassMetadata c1 = new ClassMetadata(type1, classLoader);
        ClassMetadata c2 = new ClassMetadata(type2, classLoader);

        if (c1.isAssignableFrom(c2)) {
            return type1;
        }
        if (c2.isAssignableFrom(c1)) {
            return type2;
        }
        if (c1.isInterface() || c2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c1 = c1.getSuperclass();
            } while (!c1.isAssignableFrom(c2));
            return c1.getName().replace('.', '/');
        }
    }
}
