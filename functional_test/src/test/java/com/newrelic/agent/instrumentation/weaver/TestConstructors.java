/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

public class TestConstructors {

    private final String existingField = "existingConstructorInitOkay";

    public TestConstructors() {
    }

    public TestConstructors(String t) {
        this();
    }

    public String variableInit() {
        return "";
    }

    public String getExistingField() {
        return existingField;
    }
}
