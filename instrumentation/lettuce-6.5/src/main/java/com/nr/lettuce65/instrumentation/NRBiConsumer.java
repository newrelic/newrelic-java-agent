/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce65.instrumentation;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import java.util.function.BiConsumer;

public class NRBiConsumer<T> implements BiConsumer<T, Throwable> {

    private Segment segment = null;
    private ExternalParameters params = null;

    public NRBiConsumer(Segment s, ExternalParameters p) {
        segment = s;
        params = p;
    }

    @Override
    public void accept(T t, Throwable u) {
        if (u != null) {
            NewRelic.noticeError(u);
        }

        if (segment != null) {
            if (params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        } else {
            if (params != null) {
                NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
            }
        }
    }

}
