/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import java.util.Map;

public class VariableTest {

    public String useConstant(Map<String, Object> map) {
        return "Test";
    }

    public int memberVariable(int x) {
        return x;
    }

    public double doubleMemberVariable(double d) {
        return d;
    }

    public Object test() {
        return "test";
    }

}
