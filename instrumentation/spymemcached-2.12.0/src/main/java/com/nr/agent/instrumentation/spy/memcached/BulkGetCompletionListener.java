/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.api.agent.Segment;
import net.spy.memcached.internal.BulkGetFuture;

public class BulkGetCompletionListener implements net.spy.memcached.internal.BulkGetCompletionListener {

    private Segment segment;

    public BulkGetCompletionListener(Segment segment) {
        this.segment = segment;
    }

    @Override
    public void onComplete(BulkGetFuture<?> future) throws Exception {
        segment.endAsync();
    }
}
