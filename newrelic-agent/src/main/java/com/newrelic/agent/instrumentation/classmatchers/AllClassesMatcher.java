/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

/**
 * Matches all concrete or abstract classes. Does not match interfaces.
 */
public class AllClassesMatcher extends ClassMatcher {

    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        return (cr.getAccess() & Opcodes.ACC_INTERFACE) == 0;
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        return !clazz.isInterface();
    }

    @Override
    public Collection<String> getClassNames() {
        return Collections.emptyList();
    }

}
