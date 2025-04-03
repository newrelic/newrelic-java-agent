/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.server.netty.handler.OutboundAccess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

@Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.RoutingInBoundHandler")
public abstract class RoutingInBoundHandler_Instrumentation {

    @Trace(dispatcher = true)
    public void accept(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpRequest request, CloseableByteBody body, OutboundAccess outboundAccess) {
        if(request != null) {
            StringBuffer sb = new StringBuffer();
            HttpMethod method = request.method();
            if(method != null) {
                sb.append(method.name());
            } else {
                sb.append("UnknownMethod");
            }
            sb.append(" - ");
            String uri = request.uri();
            if(uri != null) {
                sb.append(uri);
            } else {
                sb.append("UnknownURI");
            }
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Micronaut-Netty", sb.toString());
        }
        Weaver.callOriginal();
    }
}
