/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.bootstrap;

import com.agent.instrumentation.netty34.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.api.agent.weaver.internal.WeavePackageType;
import org.jboss.netty.channel.ChannelHandlerContext_Instrumentation;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;

import java.util.logging.Level;

/**
 * This isn't a netty class. This is an agent ChannelUpstreamHandler which will start a transaction on i/o read. It's
 * best to put this handler in front of "interesting" (e.g. ServerBootstrap) pipelines.
 * <p>
 * Since this class creates a tracer, its class+method name will show in the TT, hence the class name.
 */
public class NettyDispatcher {

    private static volatile NettyDispatcher instance = null;

    public static NettyDispatcher get() {
        if (null == instance) {
            synchronized (NettyDispatcher.class) {
                if (null == instance) {
                    instance = new NettyDispatcher();
                }
            }
        }
        return instance;
    }

    private NettyDispatcher() {
        AgentBridge.instrumentation.retransformUninstrumentedClass(NettyDispatcher.class);
    }

    @Trace(dispatcher = true)
    public static void upstreamDispatcher(ChannelHandlerContext_Instrumentation ctx, Object msg) {
        try {
            ctx.getPipeline().token = AgentBridge.getAgent().getTransaction().getToken();

            TracedMethod tracer = AgentBridge.getAgent().getTransaction().getTracedMethod();
            if (tracer == null) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "Unable to dispatch netty tx. No tracer."); // it happens.
            } else {
                tracer.setMetricName("NettyUpstreamDispatcher");
                AgentBridge.currentApiSource.set(WeavePackageType.INTERNAL);
                AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, true,
                        "NettyDispatcher", "NettyDispatcher");

                AgentBridge.getAgent()
                        .getLogger()
                        .log(Level.INFO, "Netty Debug: Set transaction name to NettyDispatcher for transaction: " + AgentBridge.getAgent().getTransaction());
            }

            Transaction tx = AgentBridge.getAgent().getTransaction(false);

            AgentBridge.getAgent()
                    .getLogger()
                    .log(Level.INFO, "Netty Debug: Called: NettyDispatcher.channelRead for transaction: " + tx + ". Token: " + ctx.getPipeline().token);

            if (tx != null) {
                tx.setWebRequest(new RequestWrapper((DefaultHttpRequest) msg));
            }

        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        } finally {
            AgentBridge.currentApiSource.remove();
        }
    }

}
