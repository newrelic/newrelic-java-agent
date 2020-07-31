/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

public class InstrumentImplObject implements InstrumentInterfaceObject {

    @Override
    public int getHeight() {
        return 33;
    }

}
