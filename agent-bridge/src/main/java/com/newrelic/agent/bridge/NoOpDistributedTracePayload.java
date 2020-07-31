/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public class NoOpDistributedTracePayload implements DistributedTracePayload {

    public static final DistributedTracePayload INSTANCE = new NoOpDistributedTracePayload();

    private NoOpDistributedTracePayload() {
    }

    @Override
    public String text() {
        return "";
    }

    @Override
    public String httpSafe() {
        return "";
    }

}
