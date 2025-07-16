/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce6.instrumentation;

import com.newrelic.api.agent.NewRelic;

import java.util.function.Consumer;

public class NRErrorConsumer implements Consumer<Throwable> {

    private NRHolder holder = null;

    public NRErrorConsumer(NRHolder h) {
        holder = h;
    }

    @Override
    public void accept(Throwable t) {
        NewRelic.noticeError(t);
        if (holder != null && !holder.hasEnded()) {
            holder.end();
        }
    }

}
