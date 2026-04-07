/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.buffer.ByteBufHolder;

@Weave(originalName = "io.micronaut.http.server.netty.DefaultHttpContentProcessor", type = MatchType.ExactClass)
public abstract class DefaultHttpContentProcessor_Instrumentation {

    @Trace(dispatcher = true)
    protected void onUpstreamMessage(ByteBufHolder message) {
        Weaver.callOriginal();
    }
}
