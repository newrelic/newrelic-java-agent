/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.iface;

public abstract class SampleAbstractObject implements SampleInterfaceObject {

    @Override
    public int getOtherNumber() {
        return 0;
    }

    @Override
    public long getTestLong() {
        return 0;
    }

    @Override
    public int getTestInt() {
        return 0;
    }

    @Override
    public int getTestIntWahoo() {
        return 0;
    }

    @Override
    public long getTestLongWahoo() {
        return 0;
    }

    @Override
    public abstract int getTestIntYipee();

}
