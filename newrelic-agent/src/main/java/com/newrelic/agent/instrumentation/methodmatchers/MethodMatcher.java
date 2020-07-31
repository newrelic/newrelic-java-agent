/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

/**
 * A matcher is used to match method signatures.
 */
public interface MethodMatcher {

    public static final Set<String> UNSPECIFIED_ANNOTATIONS = com.google.common.collect.ImmutableSet.of();

    /**
     * Used when the access flags are unknown. This should match all / any access flags.
     */
    public static final int UNSPECIFIED_ACCESS = -1;

    /**
     * Returns true if this matcher matches the given method.
     * 
     * @param access The method access flags.
     * @param name
     * @param desc
     * 
     */
    boolean matches(int access, String name, String desc, Set<String> annotations);

    boolean equals(Object obj);

    Method[] getExactMethods();

}
