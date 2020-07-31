/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.channel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface, originalName = "org.jboss.netty.channel.ChannelHandlerContext")
public abstract class ChannelHandlerContext_Instrumentation {

    public abstract ChannelPipeline_Instrumentation getPipeline();

}
