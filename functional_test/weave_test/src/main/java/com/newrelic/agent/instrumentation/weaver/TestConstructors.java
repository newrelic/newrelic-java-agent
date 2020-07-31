/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class TestConstructors {

    @NewField
    private String stringField = "constructorInitOkay";

    public TestConstructors() {

    }

    public TestConstructors(String t) {
        stringField = t;
        System.err.println("Test " + this);
    }

    public String variableInit() {
        Weaver.callOriginal();
        return stringField;
    }
}
