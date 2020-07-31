/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

public class RegisterContext {

    private final ContextObject context;
    private final long timeOfRegister;

    public RegisterContext(ContextObject context, long timeOfRegister) {
        super();
        this.context = context;
        this.timeOfRegister = timeOfRegister;
    }

    public ContextObject getContext() {
        return context;
    }

    public long getTimeOfRegister() {
        return timeOfRegister;
    }

}
