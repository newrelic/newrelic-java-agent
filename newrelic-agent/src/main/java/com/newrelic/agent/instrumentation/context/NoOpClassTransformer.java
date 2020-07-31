/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class NoOpClassTransformer implements ContextClassTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
            OptimizedClassMatcher.Match match) throws IllegalClassFormatException {
        return null;
    }

}
