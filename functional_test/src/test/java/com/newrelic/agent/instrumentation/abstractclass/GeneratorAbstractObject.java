/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.abstractclass;

public abstract class GeneratorAbstractObject {

    protected int value = 0;

    public abstract int generateInt();

    public abstract long generateLong();

    public void performMagic() {
        value = value * 2 + 1;
    }

    public void undoMagic() {
        value = value - 1;
        if (value != 0) {
            value = value / 2;
        }
    }
}
