/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.api.agent.Trace;

public class CustomWebDispatcher {
    public void dispatch() {
        foo();
    }

    @Trace
    private void foo() {

    }
}
