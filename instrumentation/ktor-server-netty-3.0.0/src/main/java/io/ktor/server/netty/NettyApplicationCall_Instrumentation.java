/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.netty;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

import io.ktor.server.application.Application;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

@Weave(originalName = "io.ktor.server.netty.NettyApplicationCall")
public class NettyApplicationCall_Instrumentation {

	@NewField
	public Token token = null;

	@NewField
	public Token routingToken = null;

	@NewField
	public HttpHeaders nrHttpHeaders = null;

	public NettyApplicationCall_Instrumentation(Application application, ChannelHandlerContext context, Object requestMessage) {
		if (requestMessage instanceof HttpRequest) {
			nrHttpHeaders = ((HttpRequest) requestMessage).headers();
		}
		Token t = NewRelic.getAgent().getTransaction().getToken();
		if (t != null && t.isActive()) {
			routingToken = t;
		} else if (t != null) {
			t.expire();
		}
	}
}
