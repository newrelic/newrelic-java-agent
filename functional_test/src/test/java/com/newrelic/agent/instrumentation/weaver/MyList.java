/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

public class MyList extends BaseWithConstructor implements IList<String> {

    public MyList() {
        super(new Object());
    }

    @Override
    public int count() {
        return 0;
    }

    public boolean remove(String item) {
        return false;
    }

    @Override
    public Object instanceFieldTest() {
        return "NOT_WORKING";
    }

    @Override
    public Object staticFieldTest() {
        return "NOT_WORKING";
    }

    @Override
    public int unimplementedWeaveMethodTest() {
        return this.unimplementedWeaveMethod();
    }

    @Override
    public int unimplementedWeaveMethod() {
        return 987;
    }
}
