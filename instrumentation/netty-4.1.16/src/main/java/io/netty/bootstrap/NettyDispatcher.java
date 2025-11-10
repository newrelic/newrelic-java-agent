/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.bootstrap;

import com.agent.instrumentation.netty4116.Http2RequestHeaderWrapper;
import com.agent.instrumentation.netty4116.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * This isn't a netty class. This is an agent class which will start a transaction on i/o read.
 * <p>
 * Since this class creates a tracer, its class+method name will show in the TT, hence the class name.
 */
public class NettyDispatcher {

    private static volatile NettyDispatcher instance = null;
    public static final AtomicBoolean instrumented = new AtomicBoolean(false);

    public static NettyDispatcher get() {
        if (null == instance) {
            synchronized (NettyDispatcher.class) {
                if (null == instance) {
                    instance = new NettyDispatcher();
                    instrumented.set(true);
                }
            }
        }
        return instance;
    }

    private NettyDispatcher() {
        AgentBridge.instrumentation.retransformUninstrumentedClass(NettyDispatcher.class);
    }

    /*
     * channelRead is invoked when a Netty request is decoded (see weave classes in
     * io.netty.handler.codec package). This is where a transaction is started,
     * a token is stored in the Netty context pipeline, and the request is processed.
     */
    @Trace(dispatcher = true)
    public static void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) {
        ctx.pipeline().token = AgentBridge.getAgent().getTransaction().getToken();
        TracedMethod tracer = AgentBridge.getAgent().getTransaction().getTracedMethod();
        if (tracer == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Unable to dispatch netty tx. No tracer."); // it happens.
        } else {
            tracer.setMetricName("NettyUpstreamDispatcher");
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, true, "NettyDispatcher", "NettyDispatcher");
        }

        Transaction tx = AgentBridge.getAgent().getTransaction(false);

        if (tx != null) {
            if (msg instanceof HttpRequest) {
                // HTTP/1 request
                tx.setWebRequest(new RequestWrapper((HttpRequest) msg));
            } else if (msg instanceof Http2Headers) {
                // HTTP/2 request
                tx.setWebRequest(new Http2RequestHeaderWrapper((Http2Headers) msg));
            }
        }
    }
}
