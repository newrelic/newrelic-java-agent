/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.iface;

public class SampleIndirectImplObject extends SampleImplObject {

    @Override
    public int getTestInt() {
        return 9;
    }

}
