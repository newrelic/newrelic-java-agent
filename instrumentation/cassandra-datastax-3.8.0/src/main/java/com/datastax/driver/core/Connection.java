/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.datastax.driver.core;

import java.net.InetSocketAddress;

import com.datastax.driver.core.exceptions.DriverInternalError;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * A connection to a Cassandra Node.
 * 
 * Various Async request and callbacks here: RequestHandler.sendRequest() -> Connection:374 (netty-channel).write(...)
 */
@Weave
class Connection {
    final EndPoint endPoint = Weaver.callOriginal();

    @Weave(type = MatchType.Interface)
    static class ResponseCallback {
        /**
         * Callback for exceptions
         */
        public void onException(Connection connection, Exception exception, long latency, int retryCount) {
            if (!(exception instanceof DriverInternalError)) {
                AgentBridge.privateApi.reportException(exception);
            }
            Weaver.callOriginal();
        }
    }
}
