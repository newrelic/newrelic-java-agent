/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

public abstract class BaseClass {
    public int constructorCounts = 0;
    public int recursiveCounts = 0;

    public BaseClass() {
        "base".toString();
    }

    public String baseCall() {
        return "base";
    }

    public void recursiveTest(int i) {
        "originalStart".toString();
        if (i <= 1)
            return;
        else
            recursiveTest(--i);
        "originalEnd".toString();
    }

    public abstract String childCall();

    public abstract String justTrace();
}
