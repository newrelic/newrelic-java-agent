/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * This is similar to a {@link ClassFileTransformer}, but it takes additional arguments containing some context about the
 * current class transformation including the class match results paired to this transformer. These transformers are added
 * by calling: {@link InstrumentationContextManager#addContextClassTransformer(ClassMatchVisitorFactory, ContextClassTransformer)}.
 */
public interface ContextClassTransformer {

    byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match)
            throws IllegalClassFormatException;

}
