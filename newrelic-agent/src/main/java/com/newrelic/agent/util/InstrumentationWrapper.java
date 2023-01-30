/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

/**
 * This delegating wrapper of an {@link Instrumentation} instance.
 */
public class InstrumentationWrapper extends DelegatingInstrumentation {

    public InstrumentationWrapper(Instrumentation delegate) {
        super(delegate);
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        if (Agent.LOG.isFinestEnabled()) {
            StringBuilder sb = new StringBuilder("Classes about to be retransformed: ");
            for (Class<?> current : classes) {
                sb.append(current.getName()).append(" ");
            }
            Agent.LOG.log(Level.FINEST, sb.toString());
        }
        super.retransformClasses(classes);
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
            UnmodifiableClassException {
        if (Agent.LOG.isFinestEnabled()) {
            StringBuilder sb = new StringBuilder("Classes about to be redefined: ");
            for (ClassDefinition current : definitions) {
                sb.append(current.getDefinitionClass().getName()).append(" ");
            }
            Agent.LOG.log(Level.FINEST, sb.toString());
        }
        delegate.redefineClasses(definitions);
    }
}
