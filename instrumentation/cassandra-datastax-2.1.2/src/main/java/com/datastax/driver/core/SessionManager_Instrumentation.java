/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.datastax.driver.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "com.datastax.driver.core.SessionManager")
abstract class SessionManager_Instrumentation {

    public ResultSetFuture executeAsync(Statement statement) {
        final Segment segment;

        ResultSetFuture result = Weaver.callOriginal();

        if (result != null
                && AgentBridge.getAgent().getTransaction(false) != null
                && AgentBridge.getAgent().getTransaction().isStarted()) {
            segment = NewRelic.getAgent().getTransaction().startSegment("executeAsync");
        } else {
            segment = null;
        }

        if (segment != null) {
            NewRelicChainedResultSetFuture nrfuture = new NewRelicChainedResultSetFuture(getLoggedKeyspace(), segment, result, statement);
            return nrfuture;
        }
        return result;
    }

    public abstract String getLoggedKeyspace();

}
