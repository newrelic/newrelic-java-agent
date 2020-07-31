/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.api.agent.NewRelic;

/**
 * Stores the name of a method parameter attribute and a method matcher. This is used to identify method parameters that
 * should be stored as attributes.
 * 
 * @see NewRelic#addCustomParameter(String, Number)
 * @see NewRelic#addCustomParameter(String, String)
 */
public class ParameterAttributeName {

    private final String attributeName;
    private final int index;
    private final MethodMatcher methodMatcher;

    public ParameterAttributeName(int index, String attributeName, MethodMatcher methodMatcher) {
        super();
        this.index = index;
        this.attributeName = attributeName;
        this.methodMatcher = methodMatcher;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public int getIndex() {
        return index;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    @Override
    public String toString() {
        return "ParameterAttributeName [attributeName=" + attributeName + ", index=" + index + ", methodMatcher="
                + methodMatcher + "]";
    }

}
