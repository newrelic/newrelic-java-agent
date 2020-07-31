/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

public class Child extends BaseClass {

    public Child() {
        super();
        "child".toString();
    }

    public String childCall() {
        return "child";
    }

    @Override
    public String justTrace() {
        return null;
    }
}
