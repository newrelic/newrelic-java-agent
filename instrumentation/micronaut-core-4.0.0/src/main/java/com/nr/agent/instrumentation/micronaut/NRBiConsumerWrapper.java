/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut;

import java.util.function.BiConsumer;

import com.newrelic.api.agent.NewRelic;

public class NRBiConsumerWrapper<R> implements BiConsumer<R, Throwable> {
    BiConsumer<R, Throwable> delegate = null;

    public NRBiConsumerWrapper(BiConsumer<R, Throwable> d) {
        delegate = d;
    }

    @Override
    public void accept(R r, Throwable throwable) {
        if(throwable != null) {
            NewRelic.noticeError(throwable);
        }
        if(delegate != null) {
            delegate.accept(r, throwable);
        }
    }
}