/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.abstractclass;

public class GeneratorImplObject extends GeneratorAbstractObject {

    @Override
    public int generateInt() {
        return 5;
    }

    @Override
    public long generateLong() {
        return 15;
    }

    @Override
    public void performMagic() {
        value = value * 2 + 3;
    }

}
