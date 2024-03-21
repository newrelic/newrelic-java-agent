/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.netty.channel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "io.netty.channel.ChannelHandler")
public class ChannelHandler_Instrumentation {

    /*
     * This is to solve a bug where the transaction is lost when spring webclient times out and throws an error
     * using the io.netty.handler.timeout.ReadTimeoutHandler class from netty.
     *
     * Any extra handlers used by netty will now link a transaction if available.
     *
     * -----------------------------------
     * WARNING
     * -----------------------------------
     *
     * Netty has marked this method as deprecated since 4.1
     *
     * If instrumentation verification fails for because of this class,
     * then in the new instrumentation module try instrumenting the class:
     *
     * io.netty.channel.AbstractChannelHandlerContext
     *
     * and its method:
     *
     * static void invokeExceptionCaught(final AbstractChannelHandlerContext next, final Throwable cause)
     *
     * */
    @Trace(async = true, excludeFromTransactionTrace = true)
    public void exceptionCaught(ChannelHandlerContext_Instrumentation ctx, Throwable cause) throws Exception {
        if (ctx != null &&
                ctx.pipeline().token != null && ctx.pipeline().token.isActive()) {
            ctx.pipeline().token.link();
        }
        Weaver.callOriginal();
    }
}
