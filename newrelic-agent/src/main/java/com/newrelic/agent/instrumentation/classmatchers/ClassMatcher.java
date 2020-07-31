/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import org.objectweb.asm.ClassReader;

import java.util.Collection;

/**
 * Matches Class objects and class file bytes.
 */
public abstract class ClassMatcher {
    protected static final String JAVA_LANG_OBJECT_INTERNAL_NAME = "java/lang/Object";

    /**
     * Returns true if the class bytes from the class reader definitely match this matcher.
     */
    public abstract boolean isMatch(ClassLoader loader, ClassReader cr);

    /**
     * Return true if the given class matches this rule.
     */
    public abstract boolean isMatch(Class<?> clazz);

    public abstract Collection<String> getClassNames();

    public boolean isExactClassMatcher() {
        return false;
    }
}
