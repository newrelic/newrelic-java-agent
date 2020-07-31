/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public class NoOpAsyncApi implements AsyncApi {

    @Override
    public void errorAsync(Object context, Throwable t) {
    }

    @Override
    public void suspendAsync(Object asyncContext) {
    }

    @Override
    public Transaction resumeAsync(Object asyncContext) {
        return null;
    }

    @Override
    public void completeAsync(Object asyncContext) {
    }

    @Override
    public void finishRootTracer() {
    }
}
