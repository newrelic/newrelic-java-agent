/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.handler.component;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "org.apache.solr.handler.component.SearchComponent")
public abstract class SearchComponent_Instrumentation {

    @Trace
    public void handleResponses(ResponseBuilder rb, ShardRequest sreq) {
        Weaver.callOriginal();

        NewRelic.getAgent().getLogger().log(Level.FINEST, "SearchComponent handleResponses after rb={0}, ShardRequest={1}", rb, sreq);
    }

    @Trace
    public abstract void prepare(ResponseBuilder rb);

    @Trace
    public abstract void process(ResponseBuilder rb);

}
