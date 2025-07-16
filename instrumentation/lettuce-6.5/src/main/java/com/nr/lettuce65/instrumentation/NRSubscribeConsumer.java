/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce65.instrumentation;

import org.reactivestreams.Subscription;

import java.util.function.Consumer;

public class NRSubscribeConsumer implements Consumer<Subscription> {

    private NRHolder holder = null;

    public NRSubscribeConsumer(NRHolder h) {
        holder = h;
    }

    @Override
    public void accept(Subscription t) {
        if (holder != null) {
            holder.startSegment();
        }
    }

}
