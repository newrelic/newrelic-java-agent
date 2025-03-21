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
import io.netty.buffer.ByteBufHolder;

import java.util.Collection;

@Weave(originalName = "io.micronaut.http.server.netty.AbstractHttpContentProcessor", type = MatchType.BaseClass)
public abstract class AbstractHttpContentProcessor_Instrumentation<T> {

    @Trace(dispatcher = true)
    protected abstract void onData(ByteBufHolder message, Collection<Object> out);
}
