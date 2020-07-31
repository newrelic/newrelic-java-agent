/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;

/**
 * Provides a hook to post-process weaved classes
 */
public interface WeavePostprocessor {

    ClassVisitor postprocess(String className, ClassVisitor cv, Set<String> utilityClassesInternalNames, WeavePackage weavePackage, boolean isUtilityClass);

    /**
     * An implementation of the {@link WeavePostprocessor} that does nothing.
     */
    WeavePostprocessor NO_POSTPROCESSOR = new WeavePostprocessor() {
        @Override
        public ClassVisitor postprocess(String className, ClassVisitor cv, Set<String> utilityClassesInternalNames, WeavePackage weavePackage,
                boolean isUtilityClass) {
            return cv;
        }
    };
}
