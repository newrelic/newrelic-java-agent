/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.channel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "org.jboss.netty.channel.ChannelUpstreamHandler")
public abstract class ChannelUpstreamHandler_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void handleUpstream(ChannelHandlerContext_Instrumentation ctx, ChannelEvent e) throws Exception {
        if (ctx.getPipeline().token != null) {
            ctx.getPipeline().token.link();
        }

        Weaver.callOriginal();
    }

}
