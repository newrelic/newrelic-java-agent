/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce65.instrumentation;

import reactor.core.publisher.SignalType;

import java.util.function.Consumer;

public class NRSignalTypeConsumer implements Consumer<SignalType> {

    private NRHolder holder = null;

    public NRSignalTypeConsumer(NRHolder h) {
        holder = h;
    }

    @Override
    public void accept(SignalType t) {
        if (holder != null && !holder.hasEnded()) {
            holder.end();
        }
    }

}
