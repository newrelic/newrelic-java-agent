/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.serialization;

import java.io.Serializable;

public class NoSerialMatchStartupObject implements Serializable {

    // do not put a serial version in here

    private final int value;

    public NoSerialMatchStartupObject() {
        super();
        value = 3;
    }

    public int getValue() {
        return value;
    }

    public int getDoubleValue() {
        return 2 * value;
    }

}
