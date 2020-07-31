/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class StaticField {

    @NewField
    private static double staticDouble;

    public double staticDouble(int val) {
        double tmp = staticDouble;
        staticDouble = val;

        Weaver.callOriginal();

        return tmp;
    }
}
