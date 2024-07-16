/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.bootstrap;

import com.agent.instrumentation.netty4116.Http2RequestHeaderWrapper;
import com.agent.instrumentation.netty4116.Http2RequestWrapper;
import com.agent.instrumentation.netty4116.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * This isn't a netty class. This is an agent class which will start a transaction on i/o read.
 * <p>
 * Since this class creates a tracer, its class+method name will show in the TT, hence the class name.
 */
public class NettyDispatcher {

    private static volatile NettyDispatcher instance = null;
    public static final AtomicBoolean instrumented = new AtomicBoolean(false);
    public static final AtomicInteger onHeadersReadCount = new AtomicInteger(1);
    public static final AtomicInteger channelReadCount = new AtomicInteger(1);

    public static void debug2(Http2Headers headers, String callingClass, String callingMethod) {
        try {
            Logger logger = AgentBridge.getAgent().getLogger();

            logger.log(Level.INFO, "[NettyDebug] ======= " + callingClass + "." + callingMethod + " COUNT : " + onHeadersReadCount.getAndIncrement() + " =======");

            Transaction tx = AgentBridge.getAgent().getTransaction(false);

            logger.log(Level.INFO, "[NettyDebug] txn: " + tx);
            logger.log(Level.INFO, "[NettyDebug] Http2Headers: " + headers);
        } catch (Exception ignored) {
            // ignored
        }
    }

    public static void debug(Token token, Http2Headers headers, String callingClass) {
        try {
            Logger logger = AgentBridge.getAgent().getLogger();

            logger.log(Level.INFO, "[NettyDebug] ======= " + callingClass + ".onHeadersRead COUNT : " + onHeadersReadCount.getAndIncrement() + " =======");

            logger.log(Level.INFO, "[NettyDebug] token: " + token);

            if (token != null) {
                logger.log(Level.INFO, "[NettyDebug] token.getTransaction: " + token.getTransaction());
            }
            Transaction tx = AgentBridge.getAgent().getTransaction(false);

            logger.log(Level.INFO, "[NettyDebug] txn: " + tx);
            logger.log(Level.INFO, "[NettyDebug] Http2Headers: " + headers);
        } catch (Exception ignored) {
            // ignored
        }
    }

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
        Logger logger = AgentBridge.getAgent().getLogger();

        logger.log(Level.INFO, "[NettyDebug] ======= NettyDispatcher.channelRead COUNT : " + channelReadCount.getAndIncrement() + " =======");

        logger.log(Level.INFO, "[NettyDebug] NettyDispatcher start tx");

        logger.log(Level.INFO, "[NettyDebug] NettyDispatcher.channelRead: " + msg);

        ctx.pipeline().token = AgentBridge.getAgent().getTransaction().getToken();

        logger.log(Level.INFO, "[NettyDebug] NettyDispatcher add token to ctx pipeline: " + ctx.pipeline().token);

        TracedMethod tracer = AgentBridge.getAgent().getTransaction().getTracedMethod();
        if (tracer == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Unable to dispatch netty tx. No tracer."); // it happens.
        } else {
            tracer.setMetricName("NettyUpstreamDispatcher");
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, true,
                    "NettyDispatcher", "NettyDispatcher");
        }

        Transaction tx = AgentBridge.getAgent().getTransaction(false);

        logger.log(Level.INFO, "[NettyDebug] NettyDispatcher tx: " + tx);

        if (tx != null) {
            logger.log(Level.INFO, "[NettyDebug] NettyDispatcher tx != null (process request): " + tx);

            if (msg instanceof HttpRequest) {
                tx.setWebRequest(new RequestWrapper((HttpRequest) msg));
            } else if (msg instanceof Http2HeadersFrame) {
                tx.setWebRequest(new Http2RequestWrapper((Http2HeadersFrame) msg));
            } else if (msg instanceof Http2Headers) {
                tx.setWebRequest(new Http2RequestHeaderWrapper((Http2Headers) msg));
            }
        }
    }
}
