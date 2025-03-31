/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.HttpHeaders;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Weave(originalName = "io.micronaut.http.server.netty.RoutingInBoundHandler", type = MatchType.ExactClass)
abstract class RoutingInBoundHandler_Instrumentation {

    @Trace(dispatcher = true)
    protected void channelRead0(ChannelHandlerContext ctx, io.micronaut.http.HttpRequest<?> request) {
        if (request != null) {
            HttpHeaders headers = request.getHeaders();
            if (headers != null) {
                Map<String, List<String>> headermap = headers.asMap();
                for (String key : headermap.keySet()) {
                    List<String> values = headermap.get(key);
                    NewRelic.getAgent().getLogger().log(Level.FINE, "\tHeader: {0} = {1}", key, values);
                }
            }
        }
        Weaver.callOriginal();
    }

}
