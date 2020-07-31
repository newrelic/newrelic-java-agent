/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.serialization;

import java.io.Serializable;

public class WithSerialVersionObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int value;

    public WithSerialVersionObject() {
        super();
        value = 5;
    }

    public int getValue() {
        return value;
    }

    public void theSerialWork() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }

    public void theSerialOther() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }
}
