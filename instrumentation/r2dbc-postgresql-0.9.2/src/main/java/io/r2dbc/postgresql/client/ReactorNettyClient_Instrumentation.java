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
import com.nr.instrumentation.r2dbc.postgresql092.EndpointData;
import reactor.netty.Connection;

import java.net.InetSocketAddress;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.postgresql.client.ReactorNettyClient")
public abstract class ReactorNettyClient_Instrumentation {

    @NewField
    public final EndpointData endpointData;

    private ReactorNettyClient_Instrumentation(Connection connection, ConnectionSettings settings) {
        if(connection.channel().remoteAddress() != null && connection.channel().remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress socketAddress = (InetSocketAddress) connection.channel().remoteAddress();
            endpointData = new EndpointData(socketAddress.getHostName(), socketAddress.getPort());
        } else {
            endpointData = null;
        }
    }

}
