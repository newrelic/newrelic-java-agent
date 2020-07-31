/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.custom;

import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassRetransformer {

    private final InstrumentationContextManager contextManager;
    private final Set<ClassMatchVisitorFactory> matchers;
    private CustomClassTransformer transformer;

    public ClassRetransformer(InstrumentationContextManager contextManager) {
        super();
        this.contextManager = contextManager;
        this.matchers = new HashSet<>();
    }

    /**
     * Replaces the entire set of extension matchers.
     * 
     * @param newMatchers
     */
    public synchronized void setClassMethodMatchers(List<ExtensionClassAndMethodMatcher> newMatchers) {
        this.matchers.clear();
        if (transformer != null) {
            matchers.add(transformer.getMatcher());
            transformer.destroy();
        }

        if (newMatchers.isEmpty()) {
            transformer = null;
        } else {
            transformer = new CustomClassTransformer(contextManager, newMatchers);
            matchers.add(transformer.getMatcher());
        }
    }

    /**
     * Appends a set of extension matchers. This is only called by the tests.
     * 
     * @param toAdd
     */
    public synchronized void appendClassMethodMatchers(List<ExtensionClassAndMethodMatcher> toAdd) {
        if (transformer != null) {
            toAdd.addAll(transformer.extensionPointCuts);
        }
        setClassMethodMatchers(toAdd);
    }

    public Set<ClassMatchVisitorFactory> getMatchers() {
        return matchers;
    }
}
