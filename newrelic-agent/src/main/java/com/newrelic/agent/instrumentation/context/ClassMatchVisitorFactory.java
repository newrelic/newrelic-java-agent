/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public interface ClassMatchVisitorFactory {

    /**
     * Returns a chaining class visitor that applies matching logic to the class referenced by the class reader and
     * classBeingRedefined. The match results are reported in the {@link InstrumentationContext}.
     */
    ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
            ClassVisitor cv, InstrumentationContext context);

    ClassMatchVisitorFactory NO_OP_FACTORY = (loader, classBeingRedefined, reader, cv, context) -> null;

}
