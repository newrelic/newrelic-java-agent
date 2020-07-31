/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import org.objectweb.asm.ClassVisitor;

import java.util.Set;

/**
 * Provides a hook to pre-process classes loaded by a weave package.
 */
public interface WeavePreprocessor {
    /**
     * Pre-process the class by returning a custom {@link ClassVisitor} that delegates to the provided
     * {@link ClassVisitor}.
     * 
     * @param cv {@link ClassVisitor} provided
     * @param utilityClassesInternalNames Internal names of utility classes in this weave package.
     * @return custom {@link ClassVisitor} that delegates to the provided {@link ClassVisitor} and does pre-processing
     */
    ClassVisitor preprocess(ClassVisitor cv, Set<String> utilityClassesInternalNames, WeavePackage weavePackage);

    /**
     * An implementation of the {@link WeavePreprocessor} that does nothing.
     */
    WeavePreprocessor NO_PREPROCESSOR = new WeavePreprocessor() {
        @Override
        public ClassVisitor preprocess(ClassVisitor cv, Set<String> utilityClassesInternalNames, WeavePackage weavePackage) {
            return cv;
        }
    };
}
