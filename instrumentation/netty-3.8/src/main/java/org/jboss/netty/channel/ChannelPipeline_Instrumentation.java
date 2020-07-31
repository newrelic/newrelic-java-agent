/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.channel;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.BaseClass, originalName = "org.jboss.netty.channel.ChannelPipeline")
public class ChannelPipeline_Instrumentation {

    @NewField
    public Token token;

}
