/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.serialization;

import java.io.Serializable;

public class WithSerialMatchStartupObject implements Serializable {

    private static final long serialVersionUID = 2L;

    private final int value;

    public WithSerialMatchStartupObject() {
        super();
        value = 4;
    }

    public int getValue() {
        return value;
    }

    public int getDoubleValue() {
        return 2 * value;
    }

}
