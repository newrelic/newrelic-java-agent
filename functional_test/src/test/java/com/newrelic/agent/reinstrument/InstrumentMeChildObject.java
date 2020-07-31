/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

public class InstrumentMeChildObject extends InstrumentMeObject {

    @Override
    public int getShoeSize() {
        return 8;
    }

    public int getRingSize() {
        return 3;
    }

}
