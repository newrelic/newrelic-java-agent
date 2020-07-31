/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.serialization;

import java.io.Serializable;

public class NoSerialVersionObject implements Serializable {

    // do not put a serial version in here

    private final int value;

    public NoSerialVersionObject() {
        super();
        value = 2;
    }

    public int getValue() {
        return value;
    }

    public void theNoSerialWork() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }

    public void theNoSerialOther() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }

}
