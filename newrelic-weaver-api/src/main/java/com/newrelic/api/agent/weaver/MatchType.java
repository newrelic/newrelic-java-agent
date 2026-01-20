/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver;

/**
 * The match type of a weave instrumentation class.
 */
public enum MatchType {
    /**
     * An exact class match. The weave instrumentation will be injected into the class with the exact same name as the
     * weave class.
     */
    ExactClass(true),
    /**
     * The weave instrumentation will be injected into all classes which extend a class with the exact same name as the
     * weave class.
     */
    BaseClass(false),
    /**
     * The weave instrumentation will be injected into all classes which implement an interface with the exact same name
     * as the weave class.
     */
    Interface(false);

    private final boolean exactMatch;

    private MatchType(boolean exact) {
        this.exactMatch = exact;
    }

    /**
     * Returns true if this MatchType only matches a single class.
     * 
     * @return
     */
    public boolean isExactMatch() {
        return exactMatch;
    }
}
