/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.datastax.driver.core;

import com.datastax.driver.core.exceptions.DriverInternalError;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This implementing class does not call onException, so the {@link Connection} instrumentation won't catch errors.
 */
@Weave
class DefaultResultSetFuture {

    public void onSet(Connection connection, Message.Response response, ExecutionInfo info, Statement statement,
            long latency) {
        if (response.type == Message.Response.Type.ERROR) {
            Exception e = ((Responses.Error) response).asException(connection.address);
            if (!(e instanceof DriverInternalError)) {
                AgentBridge.privateApi.reportException(e);
            }
        }
        Weaver.callOriginal();
    }

    public boolean onTimeout(Connection connection, long latency, int retryCount) {
        AgentBridge.privateApi.reportException(new ConnectionException(connection.address, "Operation timed out"));
        return Weaver.callOriginal();
    }

}
