/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.bootstrap;

import com.agent.instrumentation.netty4116.Http2RequestWrapper;
import com.agent.instrumentation.netty4116.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.Http2HeadersFrame;

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

    @Trace(dispatcher = true)
    public static void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) {
        ctx.pipeline().token = AgentBridge.getAgent().getTransaction().getToken();
        TracedMethod tracer = AgentBridge.getAgent().getTransaction().getTracedMethod();

        NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][1] NettyDispatcher.channelRead: token = " + ctx.pipeline().token);

        NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][2] NettyDispatcher.channelRead: tracer = " + tracer);

        if (tracer == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Unable to dispatch netty tx. No tracer."); // it happens.
        } else {
            tracer.setMetricName("NettyUpstreamDispatcher");
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, true,
                    "NettyDispatcher", "NettyDispatcher");

            NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][3] NettyDispatcher.channelRead: called setTransactionName");
        }

        Transaction tx = AgentBridge.getAgent().getTransaction(false);

        NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][4] NettyDispatcher.channelRead: Transaction = " + tx);

        if (tx != null) {

            NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][5] NettyDispatcher.channelRead: msg = " + msg);

            NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][6] NettyDispatcher.channelRead: (msg instanceof Http2HeadersFrame) = " + (msg instanceof Http2HeadersFrame));

            if (msg instanceof HttpRequest) {
                tx.setWebRequest(new RequestWrapper((HttpRequest) msg));
            } else if (msg instanceof Http2HeadersFrame) {
                tx.setWebRequest(new Http2RequestWrapper((Http2HeadersFrame) msg));

                NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][7] NettyDispatcher.channelRead: called tx.setWebRequest(new Http2RequestWrapper((Http2HeadersFrame) msg))");
            }
        }
    }
}
