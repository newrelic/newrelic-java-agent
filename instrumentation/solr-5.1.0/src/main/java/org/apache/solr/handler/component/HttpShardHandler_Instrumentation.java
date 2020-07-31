/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.handler.component;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.solr.handler.component.HttpShardHandler")
public abstract class HttpShardHandler_Instrumentation {

    @Trace
    public void submit(final ShardRequest sreq, final String shard, final ModifiableSolrParams params) {
        Weaver.callOriginal();

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "HttpShardHandler submit sreq={0}, shard={1}, params={2}",
                sreq, shard, params);
    }

}
