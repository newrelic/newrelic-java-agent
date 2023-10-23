/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.r2dbc.postgresql.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import io.r2dbc.postgresql.client.ConnectionSettings;
import reactor.netty.Connection;

import java.util.Optional;
import java.util.TimeZone;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.postgresql.client.ReactorNettyClient")
public abstract class ReactorNettyClient_Instrumentation {
    @NewField
    public final Connection clientConnection;

    private ReactorNettyClient_Instrumentation(Connection connection, ConnectionSettings settings) {
        this.clientConnection = connection;
    }

}
