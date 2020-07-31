/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.iface;

public class SampleImplObject implements SampleInterfaceObject {

    @Override
    public long getTestLong() {
        return 5;
    }

    @Override
    public int getTestInt() {
        return 3;
    }

    @Override
    public int getOtherNumber() {
        return 88;
    }

    @Override
    public int getTestIntWahoo() {
        return 44;
    }

    @Override
    public long getTestLongWahoo() {
        return 2;
    }

    @Override
    public int getTestIntYipee() {
        return 0;
    }

}
